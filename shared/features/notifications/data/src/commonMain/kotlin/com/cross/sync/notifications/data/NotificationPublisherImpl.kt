package com.cross.sync.notifications.data

import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.repository.NotificationPublisher
import com.cross.sync.syncing.domain.repository.SyncRepository

class NotificationPublisherImpl(
    private val syncRepository: SyncRepository,
) : NotificationPublisher {
    override suspend fun publish(notification: SyncedNotification): Result<Unit> {
        return syncRepository.sendNotification(notification)
    }

    override suspend fun remove(removal: NotificationRemoval): Result<Unit> {
        return syncRepository.removeNotification(removal)
    }
}
