package com.cross.sync.core.db.entities

import androidx.room.Entity

@Entity(
    tableName = "synced_notifications",
    primaryKeys = ["deviceId", "notificationKey"]
)
data class SyncedNotificationEntity(
    val deviceId: String,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val isRead: Boolean,
    val isOngoing: Boolean,
    val groupKey: String,
    val isGroupSummary: Boolean,
    val kind: String,
    val hasPreview: Boolean,
    val mediaJson: String,
    val actionsJson: String,
)
