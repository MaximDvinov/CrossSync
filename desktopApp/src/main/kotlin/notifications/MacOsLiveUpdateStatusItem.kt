package notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import java.awt.EventQueue
import java.io.File

@Composable
internal fun MacOsLiveUpdateStatusItem(
    notification: SyncedNotification?,
    onClick: () -> Unit,
) {
    val title = notification?.statusBarText()
    val icon = notification?.media?.notificationIcon ?: notification?.media?.appIcon

    DisposableEffect(Unit) {
        onDispose { MacOsLiveUpdateStatusItemBridge.update(null, null, null) }
    }

    SideEffect {
        MacOsLiveUpdateStatusItemBridge.update(title, icon, onClick)
    }
}

internal object MacOsLiveUpdateStatusItemBridge {
    private const val libraryName = "libCrossSyncNotificationBridge.dylib"
    private var currentState: StatusItemState? = null
    private var currentAction: (() -> Unit)? = null

    private val nativeAction = object : MacOsLiveUpdateStatusAction {
        override fun invoke() {
            currentAction?.let { action ->
                EventQueue.invokeLater(action)
            }
        }
    }

    private val library: MacOsLiveUpdateStatusNativeLibrary? by lazy {
        if (!MacOsPopupBridge.isSupported) return@lazy null

        val file = nativeLibraryFile() ?: run {
            println("CrossSync: live update status bridge was not found")
            return@lazy null
        }

        runCatching {
            Native.load(file.absolutePath, MacOsLiveUpdateStatusNativeLibrary::class.java)
        }.onFailure { error ->
            println("CrossSync: failed to load native live update status bridge: ${error.message}")
        }.getOrNull()
    }

    private fun nativeLibraryFile(): File? {
        val configuredResourcesDirectory = System.getProperty("compose.application.resources.dir")
            ?.let(::File)
            ?.resolve(libraryName)

        val developmentCandidates = listOf(
            File("desktopApp/build/compose/tmp/prepareAppResources/$libraryName"),
            File("desktopApp/build/generated/macos-notification-bridge/$libraryName"),
            File("desktopApp/build/generated/macos-notification-bridge/macos/$libraryName"),
            File("build/compose/tmp/prepareAppResources/$libraryName"),
            File("build/generated/macos-notification-bridge/$libraryName"),
            File("build/generated/macos-notification-bridge/macos/$libraryName"),
        )

        return (listOfNotNull(configuredResourcesDirectory) + developmentCandidates)
            .firstOrNull(File::isFile)
    }

    fun update(title: String?, iconBase64: String?, onClick: (() -> Unit)?) {
        val nextTitle = title?.trim().orEmpty()
        val nextIcon = iconBase64?.trim()?.takeIf(String::isNotBlank)
        currentAction = onClick

        val nextState = StatusItemState(nextTitle, nextIcon)
        if (currentState == nextState) return
        currentState = nextState

        runCatching {
            library?.crosssync_set_live_update_status_item(
                nextTitle,
                nextIcon,
                nativeAction.takeIf { nextTitle.isNotBlank() },
            )
        }.onFailure { error ->
            println("CrossSync: failed to update live update status item: ${error.message}")
        }
    }

    fun statusItemScreenPoint(): java.awt.Point? {
        return runCatching {
            val coordinates = IntArray(2)
            val found = library?.crosssync_get_live_update_status_item_position(coordinates) ?: 0
            if (found == 1) java.awt.Point(coordinates[0], coordinates[1]) else null
        }.getOrNull()
    }
}

private data class StatusItemState(
    val title: String,
    val iconBase64: String?,
)

private interface MacOsLiveUpdateStatusAction : Callback {
    fun invoke()
}

private interface MacOsLiveUpdateStatusNativeLibrary : Library {
    fun crosssync_set_live_update_status_item(
        title: String,
        iconBase64: String?,
        action: MacOsLiveUpdateStatusAction?,
    )

    fun crosssync_get_live_update_status_item_position(coordinates: IntArray): Int
}

private fun SyncedNotification.statusBarText(): String? {
    return shortCriticalText
        .trim()
        .takeIf(String::isNotBlank)
        ?.take(7)
}
