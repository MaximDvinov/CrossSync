package com.cross.sync.notifications.data

import com.cross.sync.core.db.NotificationDao
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val dao: NotificationDao,
) : NotificationRepository {
    override suspend fun upsert(notification: SyncedNotification) {
        dao.upsert(notification.toEntity())
    }

    override suspend fun markRemoved(deviceId: String, notificationKey: String, removedAt: Long) {
        dao.markRemoved(deviceId, notificationKey, removedAt)
    }

    override suspend fun markRead(deviceId: String, notificationKey: String) {
        dao.markRead(deviceId, notificationKey)
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun clearHistoryBefore(olderThanEpochMillis: Long) {
        dao.clearHistoryBefore(olderThanEpochMillis)
    }

    override fun observeNotifications(): Flow<List<SyncedNotification>> {
        return dao.observeAll().map { notifications -> notifications.map { it.toDomain() } }
    }
}
