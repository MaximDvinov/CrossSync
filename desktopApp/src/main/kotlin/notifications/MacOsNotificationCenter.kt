package notifications

import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * AppKit bridge for the existing ComposeNativeTray [NSStatusItem]. The tray owns the
 * NSStatusBarButton; its native bridge returns the button's real AppKit screen position.
 */
internal object MacOsPopupBridge {
    private const val nonActivatingPanelMask = 1L shl 7
    private const val floatingWindowLevel = 3L
    private const val moveToActiveSpace = 1L shl 1

    val isSupported: Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    /** Configure the Compose-owned native window before it becomes visible. */
    fun preparePassive(window: Window) = onAwtThread {
        nativeWindow(window)?.let { panel ->
            val styleMask = ObjectiveC.long(panel, "styleMask")
            ObjectiveC.void(panel, "setStyleMask:", styleMask or nonActivatingPanelMask)
            ObjectiveC.void(panel, "setLevel:", floatingWindowLevel)
            ObjectiveC.void(panel, "setHidesOnDeactivate:", true)
            val behavior = ObjectiveC.long(panel, "collectionBehavior")
            ObjectiveC.void(panel, "setCollectionBehavior:", behavior or moveToActiveSpace)
        }
    }

    /** Configure a popup that must receive focus changes so an outside click can dismiss it. */
    fun prepareDismissible(window: Window) = onAwtThread {
        nativeWindow(window)?.let { panel ->
            val styleMask = ObjectiveC.long(panel, "styleMask")
            ObjectiveC.void(panel, "setStyleMask:", styleMask and nonActivatingPanelMask.inv())
            ObjectiveC.void(panel, "setHidesOnDeactivate:", true)
            ObjectiveC.void(panel, "setLevel:", floatingWindowLevel)
            val behavior = ObjectiveC.long(panel, "collectionBehavior")
            ObjectiveC.void(panel, "setCollectionBehavior:", behavior or moveToActiveSpace)
        }
    }

    /** Moves the popup below the exact NSStatusBarButton and clamps it to that display. */
    fun positionBelowStatusItem(window: Window, gap: Int = 8) = onAwtThread {
        val anchor = statusItemScreenPoint()
        val screen = anchor?.let(::screenContaining)
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
        val width = window.width.coerceAtLeast(1)
        val minX = screen.x + gap
        val maxX = max(minX, screen.x + screen.width - width - gap)
        // The existing MacTray bridge returns AppKit's status-item point. Its y-axis is
        // not AWT's y-axis, so derive the menu-bar edge from the resolved AWT display.
        val menuBarHeight = 28
        val minY = screen.y + menuBarHeight + gap

        window.location = Point(
            anchor?.let { min(max(it.x - width / 2, minX), maxX) } ?: maxX,
            minY,
        )
    }

    /** Reply is an explicit interaction: turn the passive panel into a key window. */
    fun activateForReply(window: Window) = onAwtThread {
        nativeWindow(window)?.let { panel ->
            val styleMask = ObjectiveC.long(panel, "styleMask")
            ObjectiveC.void(panel, "setStyleMask:", styleMask and nonActivatingPanelMask.inv())
            ObjectiveC.void(panel, "makeKeyAndOrderFront:", Pointer.NULL)
        }
        window.toFront()
        window.requestFocus()
    }

    private fun statusItemScreenPoint(): Point? {
        return MacOsLiveUpdateStatusItemBridge.statusItemScreenPoint() ?: runCatching {
            // Fall back to the ComposeNativeTray item when the Live Update item is not ready.
            val coordinates = IntArray(2)
            val bridgeClass = Class.forName(
                "dev.nucleusframework.composenativetray.lib.mac.MacNativeBridge",
            )
            val method = bridgeClass.getDeclaredMethod(
                "nativeGetStatusItemPosition",
                IntArray::class.java,
            )
            val found = (method.invoke(null, coordinates) as Int) != 0
            if (found) Point(coordinates[0], coordinates[1]) else null
        }.getOrNull()
    }

    private fun screenContaining(point: Point): Rectangle =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .screenDevices
            .map { it.defaultConfiguration.bounds }
            .firstOrNull { it.contains(point) }
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds

    private fun nativeWindow(window: Window): Pointer? = runCatching {
        val view = Pointer(Native.getComponentID(window))
        ObjectiveC.objectValue(view, "window").takeUnless(ObjectiveC::isNull)
    }.getOrNull()

    private fun onAwtThread(action: () -> Unit) {
        if (!isSupported) return
        if (EventQueue.isDispatchThread()) action() else EventQueue.invokeAndWait(action)
    }
}

/**
 * Modern UserNotifications delivery through a Swift bridge loaded inside CrossSync.app.
 *
 * Swift owns the Objective-C blocks and delegate, which avoids the JVM crash caused by
 * manually constructing those ABI-sensitive objects through JNA.
 */
internal class MacOsSystemNotificationCenter {
    fun publish(notification: SyncedNotification) {
        MacOsNotificationBridge.publish(
            title = notification.title.ifBlank { notification.appName },
            subtitle = notification.appName.ifBlank { notification.packageName },
            body = notification.body.ifBlank { notification.title },
        )
    }
}

private interface MacOsNotificationNativeLibrary : Library {
    fun crosssync_publish_notification(title: String, subtitle: String, body: String)
}

private object MacOsNotificationBridge {
    private const val libraryName = "libCrossSyncNotificationBridge.dylib"

    private val library: MacOsNotificationNativeLibrary? by lazy {
        if (!MacOsPopupBridge.isSupported) return@lazy null

        val file = System.getProperty("compose.application.resources.dir")
            ?.let(::File)
            ?.resolve(libraryName)
            ?.takeIf(File::isFile)
            ?: return@lazy null

        runCatching {
            Native.load(file.absolutePath, MacOsNotificationNativeLibrary::class.java)
        }.onFailure { error ->
            println("CrossSync: failed to load native notification bridge: ${error.message}")
        }.getOrNull()
    }

    fun publish(title: String, subtitle: String, body: String) {
        runCatching {
            library?.crosssync_publish_notification(title, subtitle, body)
        }.onFailure { error ->
            println("CrossSync: failed to publish macOS notification: ${error.message}")
        }
    }
}

/**
 * The first state snapshot is historical. Later active notifications create or replace a
 * popup, including a notification updated in place by its source application after an action.
 */
internal class NotificationBannerCoordinator(
    private val showPopup: (SyncedNotification) -> Unit,
    private val hidePopup: (String) -> Unit,
    private val notificationDelivery: () -> com.cross.sync.setting.domain.DesktopNotificationDelivery,
) {
    private val deliveredVersions = mutableMapOf<String, Long>()
    private val systemNotificationCenter by lazy(::MacOsSystemNotificationCenter)
    private var receivedInitialSnapshot = false

    fun onNotificationsChanged(notifications: List<SyncedNotification>) {
        val activeNotifications = notifications.filter(SyncedNotification::isActive)
        // Ongoing notifications are rendered by the dedicated Live Update tray item.
        // They must not also produce a regular macOS notification/popup on every update.
        val deliveredNotifications = activeNotifications.filterNot(SyncedNotification::isOngoing)
        val activeIds = activeNotifications.mapTo(mutableSetOf()) { it.id }

        if (receivedInitialSnapshot) {
            deliveredVersions.keys
                .filterNot(activeIds::contains)
                .forEach(hidePopup)
        }

        if (receivedInitialSnapshot) {
            val delivery = notificationDelivery()
            deliveredNotifications
                .filter { notification -> deliveredVersions[notification.id] != notification.updatedAt }
                .forEach { notification ->
                    if (delivery.showsSystem) systemNotificationCenter.publish(notification)
                    if (delivery.showsPopup && MacOsPopupBridge.isSupported) showPopup(notification)
                }
        }

        deliveredVersions.clear()
        activeNotifications.forEach { notification -> deliveredVersions[notification.id] = notification.updatedAt }
        receivedInitialSnapshot = true
    }
}

private val SyncedNotification.id: String
    get() = "$deviceId:$notificationKey"

private object ObjectiveC {
    private val library = NativeLibrary.getInstance("objc")
    private val getSelector = library.getFunction("sel_registerName")
    private val messageSend = library.getFunction("objc_msgSend")

    fun objectValue(receiver: Pointer, selector: String, vararg arguments: Any?): Pointer =
        messageSend.invoke(Pointer::class.java, objcArguments(receiver, selector, *arguments)) as Pointer

    fun long(receiver: Pointer, selector: String): Long =
        (messageSend.invoke(Long::class.java, arrayOf(receiver, selector(selector))) as Number).toLong()

    fun void(receiver: Pointer, selector: String, vararg arguments: Any?) {
        messageSend.invoke(Void.TYPE, objcArguments(receiver, selector, *arguments))
    }

    fun isNull(pointer: Pointer?): Boolean = pointer == null || Pointer.nativeValue(pointer) == 0L

    private fun selector(name: String): Pointer =
        getSelector.invoke(Pointer::class.java, arrayOf(name)) as Pointer

    private fun objcArguments(receiver: Pointer, selector: String, vararg arguments: Any?): Array<Any?> =
        arrayOf(receiver, this.selector(selector), *arguments)
}
