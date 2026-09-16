package windows

import registerGlobalMousePressListener
import unregisterGlobalMousePressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.key
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationPopupCard
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.theme.AppTheme
import notifications.MacOsPopupBridge
import kotlinx.coroutines.delay
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.Window as AwtWindow
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities

/** Interactive Compose cards hosted in one passive AppKit panel below the menu-bar icon. */
@Composable
fun ApplicationScope.MacOsNotificationPopup(
    notifications: List<SyncedNotification>,
    onDismiss: (SyncedNotification) -> Unit,
    onDismissAll: () -> Unit,
) {
    val latestOnDismissAll by rememberUpdatedState(onDismissAll)
    val screenBounds = remember {
        runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
        }.getOrNull()
    }
    val width = screenBounds?.width
        ?.let { screenWidth -> (screenWidth - 32).coerceAtLeast(PopupMinWidth.value.toInt()).dp }
        ?.coerceIn(PopupMinWidth, PopupPreferredWidth)
        ?: PopupPreferredWidth
    val maxHeight = screenBounds?.height
        ?.let { screenHeight -> (screenHeight - 48).coerceAtLeast(PopupMinHeight.value.toInt()).dp }
        ?.coerceAtMost(PopupMaxHeight)
        ?: PopupMaxHeight
    val listMaxHeight = (maxHeight - PopupChromeHeight).coerceAtLeast(0.dp)
    var measuredCardHeights by remember {
        mutableStateOf<Map<PopupNotificationVersion, Int>>(emptyMap())
    }
    val density = LocalDensity.current
    val fallbackCardHeight = with(density) { PopupInitialCardHeight.roundToPx() }
    val listHeight = (
        notifications.sumOf { notification ->
            measuredCardHeights[notification.popupVersion] ?: fallbackCardHeight
        }.toDp(density) +
            PopupItemSpacing * (notifications.size - 1).coerceAtLeast(0).toFloat()
        ).coerceAtMost(listMaxHeight)
    val height = (listHeight + PopupChromeHeight).coerceIn(PopupMinHeight, maxHeight)
    val windowState = rememberWindowState(width = width, height = height)
    var nativeWindow by remember { mutableStateOf<AwtWindow?>(null) }
    var interactedNotificationVersions by remember {
        mutableStateOf<Set<PopupNotificationVersion>>(emptySet())
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(notifications.map(SyncedNotification::popupVersion)) {
        val versions = notifications.map(SyncedNotification::popupVersion).toSet()
        measuredCardHeights = measuredCardHeights.filterKeys(versions::contains)
    }

    Window(
        onCloseRequest = onDismissAll,
        state = windowState,
        visible = notifications.isNotEmpty(),
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
    ) {
        val awtWindow = window
        DisposableEffect(awtWindow) {
            nativeWindow = awtWindow
            awtWindow.minimumSize = Dimension(width.value.toInt(), PopupMinHeight.value.toInt())
            MacOsPopupBridge.prepareDismissible(awtWindow)

            fun dismissOnAwtThread() {
                if (EventQueue.isDispatchThread()) {
                    latestOnDismissAll()
                } else {
                    EventQueue.invokeLater { latestOnDismissAll() }
                }
            }

            var receivedFocus = false
            val focusListener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(event: WindowEvent) {
                    receivedFocus = true
                }

                override fun windowLostFocus(event: WindowEvent) {
                    if (receivedFocus) dismissOnAwtThread()
                }
            }
            val windowListener = object : WindowAdapter() {
                override fun windowDeactivated(event: WindowEvent) {
                    if (receivedFocus) dismissOnAwtThread()
                }
            }

            awtWindow.addWindowFocusListener(focusListener)
            awtWindow.addWindowListener(windowListener)
            onDispose {
                awtWindow.removeWindowFocusListener(focusListener)
                awtWindow.removeWindowListener(windowListener)
                if (nativeWindow === awtWindow) nativeWindow = null
            }
        }

        if (notifications.isNotEmpty()) {
            DisposableEffect(awtWindow) {
                val outsideClickListener = AWTEventListener { event ->
                    val mouseEvent = event as? MouseEvent ?: return@AWTEventListener
                    if (mouseEvent.id != MouseEvent.MOUSE_PRESSED) return@AWTEventListener
                    val sourceWindow = mouseEvent.component?.let(SwingUtilities::getWindowAncestor)
                    if (sourceWindow !== awtWindow) {
                        EventQueue.invokeLater { latestOnDismissAll() }
                    }
                }
                Toolkit.getDefaultToolkit().addAWTEventListener(
                    outsideClickListener,
                    java.awt.AWTEvent.MOUSE_EVENT_MASK,
                )
                val mouseListener = registerGlobalMousePressListener { x, y ->
                    EventQueue.invokeLater {
                        if (!awtWindow.bounds.contains(x, y)) latestOnDismissAll()
                    }
                }
                onDispose {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener)
                    unregisterGlobalMousePressListener(mouseListener)
                }
            }
        }

        AppTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = AppTheme.colors.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .background(AppTheme.colors.background)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                    notifications.forEach { notification ->
                        val version = notification.popupVersion
                        key(version) {
                            LaunchedEffect(
                                version,
                                version in interactedNotificationVersions,
                            ) {
                                if (version !in interactedNotificationVersions) {
                                    delay(8_000)
                                    onDismiss(notification)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PopupItemSpacing),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = listMaxHeight)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(PopupItemSpacing),
                        ) {
                            notifications.forEach { notification ->
                                val version = notification.popupVersion
                                key(version) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onSizeChanged { size ->
                                                if (measuredCardHeights[version] != size.height) {
                                                    measuredCardHeights = measuredCardHeights +
                                                        (version to size.height)
                                                }
                                            },
                                    ) {
                                        NotificationPopupCard(
                                            notification = notification,
                                            onDismiss = {
                                                interactedNotificationVersions =
                                                    interactedNotificationVersions - version
                                                onDismiss(notification)
                                            },
                                            onReplyRequested = {
                                                nativeWindow?.let(MacOsPopupBridge::activateForReply)
                                            },
                                            onInteraction = {
                                                interactedNotificationVersions =
                                                    interactedNotificationVersions + version
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    RoundedTextButton(
                        text = "Close",
                        onClick = {
                            interactedNotificationVersions = emptySet()
                            onDismissAll()
                        },
                        modifier = Modifier.align(Alignment.End),
                    )
            }
        }
    }

    LaunchedEffect(width, height) {
        windowState.size = DpSize(width = width, height = height)
    }

    LaunchedEffect(notifications.map(SyncedNotification::id), nativeWindow) {
        nativeWindow?.let { awtWindow ->
            // The nonactivating panel mode is restored after a previous reply made it key.
            MacOsPopupBridge.prepareDismissible(awtWindow)
            if (notifications.isNotEmpty()) MacOsPopupBridge.positionBelowStatusItem(awtWindow)
        }
    }
}

private val SyncedNotification.id: String
    get() = "$deviceId:$notificationKey"

private data class PopupNotificationVersion(
    val id: String,
    val updatedAt: Long,
)

private val SyncedNotification.popupVersion: PopupNotificationVersion
    get() = PopupNotificationVersion(id = id, updatedAt = updatedAt)

private val PopupMinWidth = 280.dp
private val PopupPreferredWidth = 360.dp
private val PopupMinHeight = 160.dp
private val PopupMaxHeight = 520.dp
private val PopupInitialCardHeight = 110.dp
private val PopupItemSpacing = 8.dp
private val PopupChromeHeight = 72.dp

private fun Int.toDp(density: androidx.compose.ui.unit.Density) = with(density) { toDp() }
