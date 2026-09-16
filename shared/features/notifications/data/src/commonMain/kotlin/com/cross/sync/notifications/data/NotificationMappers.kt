package com.cross.sync.notifications.data

import com.cross.sync.core.db.entities.SyncedNotificationEntity
import com.cross.sync.notifications.domain.entity.NotificationActionDescriptor
import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.NotificationMedia
import com.cross.sync.notifications.domain.entity.SyncedNotification
import kotlinx.serialization.json.Json

private val json = Json

fun SyncedNotification.toEntity(): SyncedNotificationEntity = SyncedNotificationEntity(
    deviceId = deviceId,
    notificationKey = notificationKey,
    packageName = packageName,
    appName = appName,
    title = title,
    body = body,
    shortCriticalText = shortCriticalText,
    postedAt = postedAt,
    updatedAt = updatedAt,
    isActive = isActive,
    isRead = isRead,
    isOngoing = isOngoing,
    groupKey = groupKey,
    isGroupSummary = isGroupSummary,
    kind = kind.name,
    hasPreview = hasPreview,
    mediaJson = media?.let { json.encodeToString(it) }.orEmpty(),
    actionsJson = json.encodeToString(actions),
)

fun SyncedNotificationEntity.toDomain(): SyncedNotification = SyncedNotification(
    deviceId = deviceId,
    notificationKey = notificationKey,
    packageName = packageName,
    appName = appName,
    title = title,
    body = body,
    shortCriticalText = shortCriticalText,
    postedAt = postedAt,
    updatedAt = updatedAt,
    isActive = isActive,
    isRead = isRead,
    isOngoing = isOngoing,
    groupKey = groupKey,
    isGroupSummary = isGroupSummary,
    kind = runCatching { NotificationKind.valueOf(kind) }.getOrDefault(NotificationKind.Other),
    hasPreview = hasPreview,
    media = mediaJson.takeIf(String::isNotBlank)
        ?.let { runCatching { json.decodeFromString<NotificationMedia>(it) }.getOrNull() },
    actions = runCatching { json.decodeFromString<List<NotificationActionDescriptor>>(actionsJson) }
        .getOrDefault(emptyList()),
)
