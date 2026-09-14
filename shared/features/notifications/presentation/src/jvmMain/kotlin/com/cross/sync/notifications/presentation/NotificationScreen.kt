package com.cross.sync.notifications.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.ArrowLeft
import org.koin.compose.koinInject

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    viewModel: NotificationViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsState()

    AppTheme {
        NotificationContent(
            modifier = modifier,
            notifications = state.notifications,
            actionError = state.actionError,
            pendingActions = state.pendingActions,
            onBack = onBack,
            onClearHistory = viewModel::clearHistory,
            onDismiss = viewModel::dismiss,
            onAction = viewModel::invoke,
            onReply = viewModel::reply,
            compact = compact,
        )
    }
}

@Composable
private fun NotificationContent(
    notifications: List<SyncedNotification>,
    actionError: String?,
    pendingActions: Set<String>,
    onBack: (() -> Unit)?,
    onClearHistory: () -> Unit,
    onDismiss: (SyncedNotification) -> Unit,
    onAction: (SyncedNotification, Int) -> Unit,
    onReply: (SyncedNotification, Int, String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (compact) 10.dp else 20.dp
    var showArchive by rememberSaveable { mutableStateOf(false) }
    val activeNotifications = notifications.filter { it.isActive }
    val archivedNotifications = notifications.filterNot { it.isActive }
    val visibleNotifications = if (showArchive) archivedNotifications else activeNotifications
    val visibleEntries = visibleNotifications.toDisplayEntries()

    Column(modifier = modifier.fillMaxSize()) {
        TopBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = if (compact) 10.dp else 16.dp),
            text = "Notifications",
            titleStyle = if (compact) {
                AppTheme.typography.semiBold20.copy(color = AppTheme.colors.primary)
            } else {
                AppTheme.typography.semiBold28.copy(color = AppTheme.colors.primary)
            },
            leftAction = onBack?.let { back ->
                {
                    RoundedIconButton(
                        imageVector = AppTheme.icons.ArrowLeft,
                        onClick = back,
                        contentDescription = "Back",
                    )
                }
            },
            rightAction = if (notifications.isEmpty()) {
                null
            } else {
                {
                    RoundedTextButton(
                        text = "Clear all",
                        onClick = onClearHistory,
                        colors = ButtonsDefaults.buttonPadding(
                            horizontal = if (compact) 10.dp else 16.dp,
                            vertical = if (compact) 4.dp else 8.dp,
                            shape = AppTheme.shapes.round50percent,
                            containerColor = AppTheme.colors.redContainer,
                            contentColor = AppTheme.colors.onRedContainer,
                        ),
                    )
                }
            },
        )

        NotificationTabs(
            showArchive = showArchive,
            activeCount = activeNotifications.size,
            archiveCount = archivedNotifications.size,
            onTabSelected = { showArchive = it },
            compact = compact,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )

        actionError?.let { error ->
            AppMessage(
                text = error,
                modifier = Modifier.padding(horizontal = horizontalPadding),
                compact = compact,
            )
        }

        if (visibleEntries.isEmpty()) {
            EmptyNotifications(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
                title = if (showArchive) "No archived notifications" else "No active notifications",
                compact = compact,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = 4.dp,
                    end = horizontalPadding,
                    bottom = horizontalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            ) {
                items(
                    items = visibleEntries,
                    key = { it.key },
                    contentType = { it.contentType },
                ) { entry ->
                    when (entry) {
                        is NotificationDisplayEntry.Single -> NotificationItem(
                            notification = entry.notification,
                            actionPending = entry.notification.actionKey() in pendingActions,
                            onDismiss = { onDismiss(entry.notification) },
                            onAction = { index -> onAction(entry.notification, index) },
                            onReply = { index, text -> onReply(entry.notification, index, text) },
                            compact = compact,
                        )

                        is NotificationDisplayEntry.Group -> NotificationGroup(
                            group = entry,
                            pendingActions = pendingActions,
                            onDismiss = onDismiss,
                            onAction = onAction,
                            onReply = onReply,
                            compact = compact,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationGroup(
    group: NotificationDisplayEntry.Group,
    pendingActions: Set<String>,
    onDismiss: (SyncedNotification) -> Unit,
    onAction: (SyncedNotification, Int) -> Unit,
    onReply: (SyncedNotification, Int, String) -> Unit,
    compact: Boolean,
) {
    var expanded by rememberSaveable(group.key) { mutableStateOf(false) }

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + expandVertically(
                expandFrom = androidx.compose.ui.Alignment.Top,
                animationSpec = tween(260),
            )) togetherWith (fadeOut(animationSpec = tween(160)) + shrinkVertically(
                shrinkTowards = androidx.compose.ui.Alignment.Top,
                animationSpec = tween(220),
            )) using SizeTransform(clip = false)
        },
        label = "notificationGroupExpansion",
    ) { isExpanded ->
        if (!isExpanded) {
            NotificationGroupItem(
                notifications = group.notifications,
                onOpen = { expanded = true },
                expanded = isExpanded,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NotificationGroupItem(
                    notifications = group.notifications,
                    onOpen = { expanded = false },
                    showMessages = false,
                    expanded = isExpanded,
                )
                group.notifications.forEach { notification ->
                    NotificationItem(
                            notification = notification,
                            actionPending = notification.actionKey() in pendingActions,
                            onDismiss = { onDismiss(notification) },
                            onAction = { index -> onAction(notification, index) },
                        onReply = { index, text -> onReply(notification, index, text) },
                        compact = compact,
                    )
                }
            }
        }
    }
}

private sealed interface NotificationDisplayEntry {
    val key: String
    val contentType: String

    data class Single(val notification: SyncedNotification) : NotificationDisplayEntry {
        override val key: String = notification.actionKey()
        override val contentType: String = "notification"
    }

    data class Group(
        override val key: String,
        val notifications: List<SyncedNotification>,
        val summary: SyncedNotification?,
    ) : NotificationDisplayEntry {
        override val contentType: String = "notification-group"
    }
}

private fun List<SyncedNotification>.toDisplayEntries(): List<NotificationDisplayEntry> {
    return groupBy { notification -> notification.groupKey.takeIf { it.isNotBlank() } ?: notification.actionKey() }
        .flatMap { (key, grouped) ->
            if (grouped.all { it.groupKey.isBlank() }) {
                grouped.map(NotificationDisplayEntry::Single)
            } else {
                val children = grouped.filterNot { it.isGroupSummary }
                val visibleNotifications = children.ifEmpty { grouped }
                if (visibleNotifications.size == 1) {
                    visibleNotifications.map(NotificationDisplayEntry::Single)
                } else {
                    listOf(
                        NotificationDisplayEntry.Group(
                            key = "group:${visibleNotifications.first().deviceId}:$key",
                            notifications = visibleNotifications,
                            summary = grouped.firstOrNull { it.isGroupSummary },
                        )
                    )
                }
            }
        }
}

private fun SyncedNotification.actionKey(): String = "$deviceId:$notificationKey"

@Composable
private fun NotificationTabs(
    showArchive: Boolean,
    activeCount: Int,
    archiveCount: Int,
    onTabSelected: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = if (compact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NotificationTab(
            text = "Active ($activeCount)",
            selected = !showArchive,
            onClick = { onTabSelected(false) },
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        NotificationTab(
            text = "Archive ($archiveCount)",
            selected = showArchive,
            onClick = { onTabSelected(true) },
            compact = compact,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NotificationTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    RoundedTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        colors = ButtonsDefaults.buttonPadding(
            horizontal = if (compact) 10.dp else 14.dp,
            vertical = if (compact) 5.dp else 7.dp,
            shape = AppTheme.shapes.round50percent,
            containerColor = if (selected) AppTheme.colors.primary else AppTheme.colors.surfaceVariant,
            contentColor = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.onSurfaceVariant,
        ),
    )
}

@Composable
private fun AppMessage(text: String, compact: Boolean, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.redContainer)
            .padding(if (compact) 8.dp else 12.dp),
        style = (if (compact) AppTheme.typography.regular11 else AppTheme.typography.regular12)
            .copy(color = AppTheme.colors.onRedContainer),
    )
}

@Composable
private fun EmptyNotifications(
    title: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
            .padding(if (compact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            text = title,
            style = (if (compact) AppTheme.typography.semiBold14 else AppTheme.typography.semiBold16)
                .copy(color = AppTheme.colors.onSurface),
        )
        BasicText(
            text = "New notifications from your phone will appear here.",
            style = (if (compact) AppTheme.typography.regular11 else AppTheme.typography.regular12)
                .copy(color = AppTheme.colors.outline),
        )
    }
}
