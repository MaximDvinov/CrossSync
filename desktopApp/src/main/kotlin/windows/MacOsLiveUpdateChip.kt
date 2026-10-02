package windows

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationPopupCard
import com.cross.sync.theme.AppTheme
import java.awt.Dimension
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import notifications.MacOsPopupBridge

/** A small, Android Live Update-like card anchored below the status-bar icon. */
@Composable
fun ApplicationScope.MacOsLiveUpdateChip(
    notification: SyncedNotification?,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val windowState = rememberWindowState(width = 360.dp, height = 220.dp)
    var nativeWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    var displayedNotification by remember { mutableStateOf(notification) }
    var anchorFraction by remember { mutableFloatStateOf(0.5f) }
    val visibility = remember { MutableTransitionState(false) }
    val shouldShow = visible && notification != null
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(notification) {
        if (notification != null) displayedNotification = notification
    }
    LaunchedEffect(shouldShow, notification?.deviceId, notification?.notificationKey, notification?.kind) {
        // Every opening, including a manual reopening, owns a fresh timeout.
        if (shouldShow && notification?.kind != NotificationKind.Call) {
            delay(LiveUpdatePopupTimeoutMillis)
            latestOnDismiss()
        }
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState, shouldShow) {
        if (!shouldShow && visibility.isIdle && !visibility.currentState) {
            displayedNotification = null
        }
    }

    Window(
        title = if (displayedNotification?.kind == NotificationKind.Call) "Call" else "Live Update",
        state = windowState,
        // Keep the window and its content alive while the exit animation runs.
        visible = shouldShow || visibility.currentState || !visibility.isIdle,
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
            MacOsPopupBridge.preparePassive(awtWindow)
            onDispose {
                if (nativeWindow === awtWindow) nativeWindow = null
            }
        }

        LaunchedEffect(awtWindow, shouldShow) {
            if (!shouldShow) return@LaunchedEffect
            // AppKit monitoring requires the visible window's native peer, not just its AWT object.
            withFrameNanos { }
            var observing = true
            try {
                MacOsPopupBridge.observeLiveUpdateDismissal(awtWindow) {
                    if (observing) latestOnDismiss()
                }
                awaitCancellation()
            } finally {
                observing = false
                MacOsPopupBridge.stopObservingLiveUpdateDismissal()
            }
        }

        val transition = rememberTransition(visibility, label = "Live Update popup visibility")
        val scale = transition.animateFloat(
            transitionSpec = {
                if (targetState) spring(dampingRatio = 0.9f, stiffness = 500f)
                else tween(180, easing = LiveUpdateExitEasing)
            },
            label = "Live Update popup scale",
        ) { shown -> if (shown) 1f else 0.24f }
        val lift = transition.animateFloat(
            transitionSpec = {
                tween(
                    if (targetState) 320 else 180,
                    easing = if (targetState) LiveUpdateEnterEasing else LiveUpdateExitEasing,
                )
            },
            label = "Live Update popup lift",
        ) { shown -> if (shown) 0f else 1f }
        val opacity = transition.animateFloat(
            transitionSpec = { tween(if (targetState) 140 else 120) },
            label = "Live Update popup opacity",
        ) { shown -> if (shown) 1f else 0f }

        AppTheme {
            displayedNotification?.let { currentNotification ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LiveUpdatePopupPadding)
                        .graphicsLayer {
                            val inset = LiveUpdatePopupPadding.toPx()
                            val pivotX = ((anchorFraction * (size.width + inset * 2) - inset) / size.width)
                                .coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(pivotX, 0f)
                            scaleX = scale.value
                            scaleY = scale.value
                            translationY = -inset * lift.value
                            alpha = opacity.value
                        }
                        .verticalScroll(rememberScrollState()),
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
                        initiallyExpanded = currentNotification.kind == NotificationKind.Call,
                        animateExpansion = false,
                        showBackground = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(shouldShow, nativeWindow) {
        nativeWindow?.let { awtWindow ->
            if (shouldShow) {
                MacOsPopupBridge.preparePassive(awtWindow)
                if (!visibility.currentState && visibility.isIdle) {
                    anchorFraction = MacOsPopupBridge.positionBelowStatusItem(awtWindow)
                    windowState.position = WindowPosition(awtWindow.x.dp, awtWindow.y.dp)
                }
                displayedNotification = notification
            }
            visibility.targetState = shouldShow
        }
    }
}

private const val LiveUpdatePopupTimeoutMillis = 4_000L
private val LiveUpdatePopupPadding = 10.dp
private val LiveUpdateEnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val LiveUpdateExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
