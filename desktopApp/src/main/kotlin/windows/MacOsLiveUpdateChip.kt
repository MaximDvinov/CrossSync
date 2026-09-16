package windows

import registerGlobalMousePressListener
import unregisterGlobalMousePressListener
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationPopupCard
import com.cross.sync.theme.AppTheme
import notifications.MacOsPopupBridge
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Window as AwtWindow
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/** A small, Android Live Update-like card anchored below the status-bar icon. */
@Composable
fun ApplicationScope.MacOsLiveUpdateChip(
    notification: SyncedNotification?,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val windowState = rememberWindowState(width = 360.dp, height = 220.dp)
    var nativeWindow by remember { mutableStateOf<AwtWindow?>(null) }
    val shouldShow = visible && notification != null
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    Window(
        title = "Live Update",
        state = windowState,
        visible = shouldShow,
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
        onCloseRequest = onDismiss,
    ) {
        val awtWindow = window
        DisposableEffect(awtWindow) {
            nativeWindow = awtWindow
            awtWindow.minimumSize = Dimension(360, 220)
            MacOsPopupBridge.prepareDismissible(awtWindow)
            var receivedFocus = false
            val focusListener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(event: WindowEvent) {
                    receivedFocus = true
                }

                override fun windowLostFocus(event: WindowEvent) {
                    if (receivedFocus) latestOnDismiss()
                }
            }
            val windowListener = object : WindowAdapter() {
                override fun windowDeactivated(event: WindowEvent) {
                    if (receivedFocus) latestOnDismiss()
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

        if (shouldShow) {
            DisposableEffect(awtWindow) {
                val mouseListener = registerGlobalMousePressListener { x, y ->
                    if (!awtWindow.bounds.contains(x, y)) {
                        EventQueue.invokeLater { latestOnDismiss() }
                    }
                }
                onDispose { unregisterGlobalMousePressListener(mouseListener) }
            }
        }

        AppTheme {
            notification?.let { currentNotification ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                ) {
                    NotificationPopupCard(
                        notification = currentNotification,
                        onDismiss = onDismiss,
                        onReplyRequested = {
                            nativeWindow?.let(MacOsPopupBridge::activateForReply)
                        },
                        onInteraction = {},
                        compact = true,
                        showFullText = false,
                        initiallyExpanded = false,
                        animateExpansion = false,
                        showBackground = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(shouldShow, notification?.deviceId, notification?.notificationKey, nativeWindow) {
        nativeWindow?.let { awtWindow ->
            MacOsPopupBridge.prepareDismissible(awtWindow)
            if (shouldShow) MacOsPopupBridge.positionBelowStatusItem(awtWindow)
        }
    }
}
