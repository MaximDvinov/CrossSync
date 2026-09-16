package com.cross.sync.notifications.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import crosssync.shared.features.notifications.presentation.generated.resources.Res
import crosssync.shared.features.notifications.presentation.generated.resources.ic_notification_group_arrow
import crosssync.shared.features.notifications.presentation.generated.resources.ic_notification_send
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image as SkiaImage
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.graphics.toComposeImageBitmap

@Composable
internal fun NotificationItem(
    notification: SyncedNotification,
    actionPending: Boolean = false,
    onDismiss: () -> Unit = {},
    onAction: (Int) -> Unit,
    onReply: (Int, String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    NotificationCard(
        notification = notification,
        actionPending = actionPending,
        onDismiss = onDismiss,
        onAction = onAction,
        onReply = onReply,
        compact = compact,
        modifier = modifier,
    )
}

/** Shared card used by both the notification list and the macOS menu-bar popup. */
@Composable
fun NotificationCard(
    notification: SyncedNotification,
    actionPending: Boolean = false,
    onDismiss: () -> Unit = {},
    onAction: (Int) -> Unit,
    onReply: (Int, String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onReplyRequested: () -> Unit = {},
    onInteraction: () -> Unit = {},
    showBackground: Boolean = true,
    initiallyExpanded: Boolean = false,
    showFullText: Boolean = false,
    animateExpansion: Boolean = true,
) {
    var replyActionIndex by remember(notification.deviceId, notification.notificationKey) { mutableStateOf<Int?>(null) }
    var actionsVisible by remember(
        notification.deviceId,
        notification.notificationKey,
        initiallyExpanded,
    ) { mutableStateOf(initiallyExpanded) }
    val replyAction = notification.actions.firstOrNull { it.index == replyActionIndex }

    LaunchedEffect(actionsVisible) {
        if (!actionsVisible && replyActionIndex != null) {
            delay(220.milliseconds)
            replyActionIndex = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (animateExpansion) {
                    Modifier.animateContentSize(animationSpec = tween(220))
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NotificationContent(
            notification = notification,
            onClick = {
                onInteraction()
                val nextVisible = !actionsVisible
                actionsVisible = nextVisible
            },
            shape = if (actionsVisible && replyAction != null) NotificationTopShape else AppTheme.shapes.round10,
            compact = compact,
            showFullText = showFullText,
            showBackground = showBackground,
        )
        AnimatedVisibility(
            visible = notification.isActive && actionsVisible,
            enter = if (animateExpansion) {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 2 },
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + scaleIn(
                    initialScale = 0.88f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(animationSpec = tween(140))
            } else {
                EnterTransition.None
            },
            exit = if (animateExpansion) {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight / 3 },
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(180),
                ) + scaleOut(
                    targetScale = 0.94f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = tween(160),
                ) + fadeOut(animationSpec = tween(120))
            } else {
                ExitTransition.None
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedVisibility(
                    visible = replyAction != null,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(200),
                    ) + fadeIn(animationSpec = tween(160)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(160),
                    ) + fadeOut(animationSpec = tween(120)),
                ) {
                    replyAction?.let { action ->
                        ReplyEditor(
                            label = action.title.ifBlank { "Input" },
                            onReply = { text ->
                                onInteraction()
                                onReply(action.index, text)
                                replyActionIndex = null
                            },
                            compact = compact,
                        )
                    }
                }
                NotificationActions(
                    notification = notification,
                    actionPending = actionPending,
                    onDismiss = onDismiss,
                    onAction = { actionIndex ->
                        onInteraction()
                        onAction(actionIndex)
                    },
                    compact = compact,
                    onReplyRequested = {
                        onInteraction()
                        onReplyRequested()
                    },
                    replyActionIndex = replyActionIndex,
                    onReplyActionSelected = { replyActionIndex = it },
                )
            }
        }
    }
}

@Composable
private fun NotificationContent(
    notification: SyncedNotification,
    onClick: () -> Unit,
    shape: Shape,
    compact: Boolean,
    showMetadata: Boolean = true,
    showFullText: Boolean = false,
    showBackground: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (showBackground) Modifier.background(AppTheme.colors.surface) else Modifier)
            .clickable(enabled = notification.isActive, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showMetadata) {
            NotificationMetadata(notification = notification)
        }
        val avatar = notification.media?.avatar
        if (avatar == null) {
            NotificationText(
                notification = notification,
                compact = compact,
                showFullText = showFullText,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                NotificationBitmap(
                    encoded = avatar,
                    modifier = Modifier.size(36.dp).clip(AppTheme.shapes.round50percent),
                    contentDescription = "Notification avatar",
                    contentScale = ContentScale.Crop,
                )
                NotificationText(
                    notification = notification,
                    compact = compact,
                    showFullText = showFullText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        notification.media?.image?.let { encoded ->
            NotificationBitmap(
                encoded = encoded,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (compact) 120.dp else 220.dp)
                    .clip(AppTheme.shapes.round10),
                contentDescription = "Notification image",
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun NotificationText(
    notification: SyncedNotification,
    compact: Boolean,
    modifier: Modifier = Modifier,
    showFullText: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (notification.title.isNotBlank()) {
            BasicText(
                text = notification.title,
                maxLines = if (showFullText) Int.MAX_VALUE else if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.medium14.copy(color = AppTheme.colors.onSurface),
            )
        }
        if (notification.body.isNotBlank()) {
            BasicText(
                text = notification.body,
                maxLines = if (showFullText) Int.MAX_VALUE else if (compact) 3 else 5,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.regular12.copy(color = AppTheme.colors.onSurface),
            )
        }
    }
}

@Composable
internal fun NotificationGroupItem(
    notifications: List<SyncedNotification>,
    onOpen: (SyncedNotification) -> Unit,
    modifier: Modifier = Modifier,
    showMessages: Boolean = true,
    expanded: Boolean = false,
) {
    val firstNotification = notifications.firstOrNull() ?: return
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(180),
        label = "notificationGroupArrowRotation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
            .clickable { onOpen(firstNotification) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = firstNotification.appName,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.regular10.copy(color = AppTheme.colors.onSurfaceVariant),
                )
                BasicText(
                    text = firstNotification.postedAt.notificationDateFormat(),
                    modifier = Modifier.widthIn(min = 51.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.regular10.copy(
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            Row(
                modifier = Modifier
                    .clip(AppTheme.shapes.round50percent)
                    .background(AppTheme.colors.surfaceVariant)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = notifications.size.toString(),
                    modifier = Modifier.padding(start = 2.dp),
                    style = AppTheme.typography.regular10.copy(
                        color = AppTheme.colors.onSurface,
                        fontSize = 8.sp,
                    ),
                )
                Image(
                    painter = painterResource(Res.drawable.ic_notification_group_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .size(6.dp)
                        .rotate(arrowRotation),
                )
            }
        }
        if (showMessages) {
            notifications.forEach { notification ->
                NotificationGroupMessage(notification = notification)
            }
        }
    }
}

@Composable
private fun NotificationGroupMessage(notification: SyncedNotification) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (notification.title.isNotBlank()) {
            BasicText(
                text = notification.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.medium12.copy(color = AppTheme.colors.onSurface),
            )
        }
        if (notification.body.isNotBlank()) {
            BasicText(
                text = notification.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.regular10.copy(color = AppTheme.colors.onSurface),
            )
        }
    }
}

/** Interactive single-notification variant for transient desktop surfaces. */
@Composable
fun NotificationPopupCard(
    notification: SyncedNotification,
    onDismiss: () -> Unit,
    onReplyRequested: () -> Unit,
    onInteraction: () -> Unit = {},
    viewModel: NotificationViewModel = koinInject(),
    compact: Boolean = true,
    showFullText: Boolean = true,
    initiallyExpanded: Boolean = true,
    animateExpansion: Boolean = false,
    showBackground: Boolean = true,
) {
    val state by viewModel.state.collectAsState()
    val actionKey = "${notification.deviceId}:${notification.notificationKey}"
    NotificationCard(
        notification = notification,
        actionPending = actionKey in state.pendingActions,
        onDismiss = {
            viewModel.dismiss(notification)
            onDismiss()
        },
        onAction = { actionIndex ->
            viewModel.invoke(notification, actionIndex)
        },
        onReply = { actionIndex, text ->
            viewModel.reply(notification, actionIndex, text)
        },
        onReplyRequested = onReplyRequested,
        onInteraction = onInteraction,
        compact = compact,
        initiallyExpanded = initiallyExpanded,
        showFullText = showFullText,
        animateExpansion = animateExpansion,
        showBackground = showBackground,
    )
}

@Composable
private fun NotificationMetadata(
    notification: SyncedNotification,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        notification.media?.takeIf { it.appIcon != null || it.notificationIcon != null }?.let { media ->
            Box(modifier = Modifier.size(24.dp)) {
                media.appIcon?.let { encoded ->
                    NotificationBitmap(
                        encoded = encoded,
                        modifier = Modifier.size(24.dp).clip(AppTheme.shapes.round50percent),
                        contentDescription = "${notification.appName} icon",
                    )
                }
//                media.notificationIcon?.let { encoded ->
//                    NotificationBitmap(
//                        encoded = encoded,
//                        modifier = Modifier
//                            .size(14.dp)
//                            .align(Alignment.BottomEnd)
//                            .padding(1.5.dp)
//                            .clip(AppTheme.shapes.round50percent)
//                            .background(AppTheme.colors.surface)
//                            .border(0.dp, AppTheme.colors.surface, AppTheme.shapes.round50percent),
//                        contentDescription = "Notification icon",
//                        colorFilter = ColorFilter.tint(AppTheme.colors.onSurface),
//                    )
//                }
            }
        }
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            BasicText(
                text = notification.appName,
                modifier = Modifier,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.medium12.copy(color = AppTheme.colors.onSurfaceVariant),
            )
        }

        BasicText(
            text = notification.postedAt.notificationDateFormat(),
            modifier = Modifier.fillMaxHeight(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTheme.typography.regular10.copy(
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.End,
            ),
        )
    }
}

@Composable
private fun NotificationBitmap(
    encoded: String,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
) {
    val bitmap = remember(encoded) { encoded.toNotificationImageBitmap() } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier,
    )
}

private fun String.toNotificationImageBitmap(): ImageBitmap? = runCatching {
    SkiaImage.makeFromEncoded(Base64.decode(this)).toComposeImageBitmap()
}.getOrNull()

@Composable
private fun NotificationActions(
    notification: SyncedNotification,
    actionPending: Boolean,
    onDismiss: () -> Unit,
    onAction: (Int) -> Unit,
    compact: Boolean,
    onReplyRequested: () -> Unit,
    replyActionIndex: Int?,
    onReplyActionSelected: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        notification.actions.filterNot { it.supportsReply }.forEach { action ->
            NotificationActionChip(
                text = action.title,
                onClick = { onAction(action.index) },
                compact = compact,
                enabled = !actionPending,
            )
        }
        notification.actions.filter { it.supportsReply }.forEach { action ->
            NotificationActionChip(
                text = action.title.ifBlank { "Reply" },
                onClick = {
                    onReplyRequested()
                    onReplyActionSelected(action.index)
                },
                compact = compact,
                enabled = !actionPending,
                primary = replyActionIndex == action.index,
            )
        }
        NotificationDismissButton(
            onClick = onDismiss,
            enabled = !actionPending,
        )
    }
}

@Composable
private fun NotificationActionChip(
    text: String,
    onClick: () -> Unit,
    compact: Boolean,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    RoundedTextButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(20.dp)
            .widthIn(max = if (compact) 190.dp else 240.dp),
        colors = ButtonsDefaults.buttonPadding(
            horizontal = 8.dp,
            vertical = 2.dp,
            shape = AppTheme.shapes.round50percent,
            containerColor = if (primary) AppTheme.colors.primary else AppTheme.colors.surfaceVariant,
            contentColor = if (primary) AppTheme.colors.onPrimary else AppTheme.colors.onSurfaceVariant,
        ),
        textStyle = AppTheme.typography.regular12Center.copy(
            color = if (primary) AppTheme.colors.onPrimary else AppTheme.colors.onSurfaceVariant,
        ),
    )
}

@Composable
private fun NotificationDismissButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    RoundedIconButton(
        imageVector = AppTheme.icons.Close,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(20.dp),
        colors = ButtonsDefaults.buttonPadding(
            horizontal = 2.dp,
            vertical = 2.dp,
            shape = AppTheme.shapes.round50percent,
            containerColor = AppTheme.colors.surfaceVariant,
            contentColor = AppTheme.colors.onSurfaceVariant,
        ),
        contentDescription = "Dismiss notification",
    )
}

@Composable
private fun ReplyEditor(
    label: String,
    onReply: (String) -> Unit,
    compact: Boolean,
) {
    var text by remember(label) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(label) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .focusRequester(focusRequester)
                .clip(NotificationReplyInputShape)
                .background(AppTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            textStyle = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
            decorationBox = { innerTextField ->
                if (text.isBlank()) {
                    BasicText(
                        text = label,
                        style = AppTheme.typography.regular14.copy(color = AppTheme.colors.outline),
                    )
                }
                innerTextField()
            },
        )
        RoundedIconButton(
            painter = painterResource(Res.drawable.ic_notification_send),
            onClick = {
                onReply(text)
                text = ""
            },
            modifier = Modifier.size(30.dp),
            enabled = text.isNotBlank(),
            colors = ButtonsDefaults.buttonPadding(
                horizontal = 5.dp,
                vertical = 5.dp,
                shape = NotificationSendShape,
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.primary,
            ),
            contentDescription = "Send reply",
        )
    }
}

private val NotificationTopShape = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp,
)

private val NotificationReplyInputShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 10.dp,
    bottomEnd = 10.dp,
)

private val NotificationSendShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 4.dp,
    bottomEnd = 10.dp,
)

private fun Long.notificationDateFormat(nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedMillis = nowMillis - this
    val minuteMillis = 60_000L
    val hourMillis = 60 * minuteMillis

    return when {
        elapsedMillis < minuteMillis -> "now"
        elapsedMillis < hourMillis -> {
            val minutes = elapsedMillis / minuteMillis
            if (minutes == 1L) "1 min ago" else "$minutes min ago"
        }
        else -> {
            val zone = ZoneId.systemDefault()
            val notificationDateTime = Instant.ofEpochMilli(this).atZone(zone)
            val currentDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            if (notificationDateTime.toLocalDate() == currentDate) {
                notificationDateTime.format(NotificationTimeFormatter)
            } else {
                notificationDateTime.format(NotificationDateTimeFormatter)
            }
        }
    }
}

private val NotificationTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val NotificationDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
