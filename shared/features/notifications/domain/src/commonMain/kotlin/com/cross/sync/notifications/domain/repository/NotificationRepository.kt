package com.cross.sync.notifications.domain.repository

import com.cross.sync.notifications.domain.entity.SyncedNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun upsert(notification: SyncedNotification)
    suspend fun markRemoved(deviceId: String, notificationKey: String, removedAt: Long)
    suspend fun markRead(deviceId: String, notificationKey: String)
    suspend fun clear()
    suspend fun clearHistoryBefore(olderThanEpochMillis: Long)
    fun observeNotifications(): Flow<List<SyncedNotification>>
}
