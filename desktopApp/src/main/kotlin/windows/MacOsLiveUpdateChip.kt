package windows

import registerGlobalMousePressListener
import unregisterGlobalMousePressListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.theme.AppTheme
import notifications.MacOsPopupBridge
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Window as AwtWindow
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.io.encoding.Base64

/** A small, Android Live Update-like card anchored below the status-bar icon. */
@Composable
fun ApplicationScope.MacOsLiveUpdateChip(
    notification: SyncedNotification?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val windowState = rememberWindowState(width = 320.dp, height = 78.dp)
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
            awtWindow.minimumSize = Dimension(320, 78)
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
                LiveUpdateChipContent(
                    notification = currentNotification,
                    onClick = onOpenNotifications,
                )
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

@Composable
private fun LiveUpdateChipContent(
    notification: SyncedNotification,
    onClick: () -> Unit,
) {
    val icon = remember(notification.media?.notificationIcon) {
        notification.media?.notificationIcon?.toImageBitmap()
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.2f), shape)
            .background(AppTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = "Notification icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            BasicText(
                text = notification.appName.ifBlank { notification.packageName },
                maxLines = 1,
                style = AppTheme.typography.regular10.copy(
                    color = AppTheme.colors.onSurfaceVariant,
                ),
            )
            BasicText(
                text = notification.liveUpdateSummary(),
                maxLines = 1,
                style = AppTheme.typography.medium12.copy(color = AppTheme.colors.onSurface),
            )
        }
    }
}

private fun String.toImageBitmap(): ImageBitmap? = runCatching {
    SkiaImage.makeFromEncoded(Base64.decode(this)).toComposeImageBitmap()
}.getOrNull()

private fun SyncedNotification.liveUpdateSummary(): String {
    val title = title.trim().ifBlank { "Live Update" }
    val body = body.trim()
    return if (body.isBlank()) title else "$title · $body"
        .replace(Regex("\\s+"), " ")
        .take(96)
}
