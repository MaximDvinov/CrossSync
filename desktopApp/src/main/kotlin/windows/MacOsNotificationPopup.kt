package windows

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationPopupCard
import com.cross.sync.theme.AppTheme
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.delay
import notifications.MacOsPopupBridge

/** Interactive Compose cards hosted in one passive AppKit panel below the menu-bar icon. */
@Composable
fun ApplicationScope.MacOsNotificationPopup(
    notifications: List<SyncedNotification>,
    onDismiss: (SyncedNotification) -> Unit,
    onDismissAll: () -> Unit,
) {
    // Keep the last content and its measurements until the exit finishes.
    var displayedNotifications by remember { mutableStateOf(notifications) }
    val visibility = remember { MutableTransitionState(false) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    var anchorFraction by remember { mutableFloatStateOf(0.5f) }
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
    val windowState = rememberWindowState(width = width, height = PopupMinHeight)
    var nativeWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    var interactedNotificationVersions by remember {
        mutableStateOf<Set<PopupNotificationVersion>>(emptySet())
    }
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            displayedNotifications = notifications
        } else {
            visibility.targetState = false
        }
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState, notifications.isEmpty()) {
        if (notifications.isEmpty() && visibility.isIdle && !visibility.currentState) {
            displayedNotifications = emptyList()
            interactedNotificationVersions = emptySet()
            scrollState.scrollTo(0)
        }
    }
    LaunchedEffect(displayedNotifications.map(SyncedNotification::popupVersion)) {
        val versions = displayedNotifications.map(SyncedNotification::popupVersion).toSet()
        interactedNotificationVersions = interactedNotificationVersions.filter(versions::contains).toSet()
    }

    // Timers follow live data, so retained exit content cannot dispatch another dismissal.
    notifications.forEach { notification ->
        val version = notification.popupVersion
        key(version) {
            LaunchedEffect(version, hovered, version in interactedNotificationVersions) {
                if (!hovered && version !in interactedNotificationVersions) {
                    delay(PopupTimeoutMillis)
                    latestOnDismiss(notification)
                }
            }
        }
    }

    Window(
        onCloseRequest = onDismissAll,
        state = windowState,
        visible = displayedNotifications.isNotEmpty(),
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
    ) {
        val awtWindow = window
        DisposableEffect(awtWindow) {
            nativeWindow = awtWindow
            awtWindow.minimumSize = Dimension(width.value.toInt(), PopupMinHeight.value.toInt())
            MacOsPopupBridge.preparePassive(awtWindow)
            onDispose {
                if (nativeWindow === awtWindow) nativeWindow = null
            }
        }

        // Measure scroll content in this window's density, including its full natural height.
        // Resizing is driven by the same frame clock that draws the popup.
        var measuredListHeight by remember { mutableStateOf(PopupInitialCardHeight) }
        val density = LocalDensity.current
        val targetHeight = (measuredListHeight + PopupChromeHeight)
            .coerceIn(PopupMinHeight, maxHeight)
        val height by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(PopupResizeMillis, easing = PopupResizeEasing),
            label = "Notification popup height",
        )
        LaunchedEffect(width, height) {
            windowState.size = DpSize(width, height)
        }

        val transition = rememberTransition(visibility, label = "Notification popup visibility")
        val scale = transition.animateFloat(
            transitionSpec = {
                if (targetState) spring(dampingRatio = 0.9f, stiffness = 500f)
                else tween(180, easing = PopupExitEasing)
            },
            label = "Notification popup scale",
        ) { visible -> if (visible) 1f else 0.24f }
        val lift = transition.animateFloat(
            transitionSpec = {
                tween(if (targetState) 320 else 180, easing = if (targetState) PopupEnterEasing else PopupExitEasing)
            },
            label = "Notification popup lift",
        ) { visible -> if (visible) 0f else 1f }
        val opacity = transition.animateFloat(
            transitionSpec = { tween(if (targetState) 140 else 120) },
            label = "Notification popup opacity",
        ) { visible -> if (visible) 1f else 0f }

        AppTheme {
            val shape = RoundedCornerShape(20.dp)
            Box(Modifier.fillMaxSize().padding(PopupOuterPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Account for screen-edge clamping: the icon need not be centered.
                            val inset = PopupOuterPadding.toPx()
                            val pivotX = ((anchorFraction * (size.width + inset * 2) - inset) / size.width)
                                .coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(pivotX, 0f)
                            scaleX = scale.value
                            scaleY = scale.value
                            translationY = -inset * lift.value
                            alpha = opacity.value
                        }
                        .shadow(12.dp, shape, clip = false)
                        .clip(shape)
                        .background(AppTheme.colors.background)
                        .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.16f), shape)
                        .hoverable(hoverInteractionSource)
                        .padding(PopupInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(PopupItemSpacing),
                ) {
                    NotificationPopupHeader(
                        count = displayedNotifications.size,
                        onDismissAll = onDismissAll,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .heightIn(max = listMaxHeight)
                            .verticalScroll(scrollState),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    measuredListHeight = with(density) { size.height.toDp() }
                                },
                            verticalArrangement = Arrangement.spacedBy(PopupItemSpacing),
                        ) {
                            displayedNotifications.forEach { notification ->
                                val version = notification.popupVersion
                                // A payload update keeps reply/expansion state for the same notification.
                                key(notification.id) {
                                    // Existing cards remain still while a new card is revealed below them.
                                    var entered by remember { mutableStateOf(!visibility.currentState) }
                                    val cardOpacity = animateFloatAsState(
                                        targetValue = if (entered) 1f else 0f,
                                        animationSpec = tween(PopupResizeMillis),
                                        label = "Notification card appearance",
                                    )
                                    LaunchedEffect(Unit) { entered = true }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { alpha = cardOpacity.value },
                                    ) {
                                        NotificationPopupCard(
                                            notification = notification,
                                            onDismiss = { onDismiss(notification) },
                                            onReplyRequested = {
                                                nativeWindow?.let(MacOsPopupBridge::activateForReply)
                                            },
                                            onInteraction = {
                                                interactedNotificationVersions = interactedNotificationVersions + version
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(notifications.isNotEmpty(), nativeWindow, width) {
        nativeWindow?.let { awtWindow ->
            // The nonactivating panel mode is restored after a previous reply made it key.
            MacOsPopupBridge.preparePassive(awtWindow)
            if (notifications.isNotEmpty()) {
                // Choose an anchor only when opening, not on every list update.
                if (!visibility.currentState && visibility.isIdle) {
                    anchorFraction = MacOsPopupBridge.positionBelowStatusItem(awtWindow)
                    windowState.position = WindowPosition(awtWindow.x.dp, awtWindow.y.dp)
                }
                visibility.targetState = true
            }
        }
    }
}

@Composable
private fun NotificationPopupHeader(
    count: Int,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(PopupHeaderHeight),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "Notifications",
            style = AppTheme.typography.medium12.copy(color = AppTheme.colors.onSurface),
        )
        BasicText(
            text = count.toString(),
            style = AppTheme.typography.regular11.copy(color = AppTheme.colors.onSurfaceVariant),
            modifier = Modifier
                .background(AppTheme.colors.surfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Box(Modifier.weight(1f))
        BasicText(
            text = if (count > 1) "Close all" else "Close",
            style = AppTheme.typography.regular12.copy(color = AppTheme.colors.onSurfaceVariant),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onDismissAll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
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
private val PopupPreferredWidth = 380.dp
private val PopupMinHeight = 160.dp
private val PopupMaxHeight = 520.dp
private val PopupInitialCardHeight = 110.dp
private val PopupItemSpacing = 8.dp
private val PopupOuterPadding = 12.dp
private val PopupInnerPadding = 10.dp
private val PopupHeaderHeight = 28.dp
private val PopupChromeHeight = PopupOuterPadding * 2 + PopupInnerPadding * 2 + PopupHeaderHeight + PopupItemSpacing
private const val PopupTimeoutMillis = 8_000L
private const val PopupResizeMillis = 300
private val PopupResizeEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val PopupEnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val PopupExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
