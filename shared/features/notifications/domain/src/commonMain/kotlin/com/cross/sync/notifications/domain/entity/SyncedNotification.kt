package com.cross.sync.notifications.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class SyncedNotification(
    val deviceId: String = "",
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val shortCriticalText: String = "",
    val postedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true,
    val isRead: Boolean = false,
    val isOngoing: Boolean = false,
    val groupKey: String = "",
    val isGroupSummary: Boolean = false,
    val kind: NotificationKind = NotificationKind.Other,
    val hasPreview: Boolean = true,
    val media: NotificationMedia? = null,
    val actions: List<NotificationActionDescriptor> = emptyList(),
)

@Serializable
data class NotificationMedia(
    /** The source application's launcher icon. */
    val appIcon: String? = null,
    /** The notification's small/status-bar icon. */
    val notificationIcon: String? = null,
    /** A large image from BigPictureStyle or an equivalent notification template. */
    val image: String? = null,
    /** A conversation participant avatar, when the notification provides one. */
    val avatar: String? = null,
)

@Serializable
enum class NotificationKind {
    Message,
    Inbox,
    Call,
    Media,
    Progress,
    Image,
    Custom,
    Other,
}

@Serializable
data class NotificationActionDescriptor(
    val index: Int,
    val title: String,
    val supportsReply: Boolean,
)

@Serializable
data class NotificationRemoval(
    val notificationKey: String,
    val removedAt: Long,
)

@Serializable
data class NotificationActionRequest(
    val deviceId: String,
    val notificationKey: String,
    val action: NotificationAction,
)

@Serializable
sealed class NotificationAction {
    @Serializable
    data object Open : NotificationAction()

    @Serializable
    data object Dismiss : NotificationAction()

    @Serializable
    data class Invoke(val index: Int) : NotificationAction()

    @Serializable
    data class Reply(val index: Int, val text: String) : NotificationAction()
}
