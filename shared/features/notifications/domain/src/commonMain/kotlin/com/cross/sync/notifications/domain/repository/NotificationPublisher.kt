package com.cross.sync.notifications.domain.repository

import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.NotificationSnapshot
import com.cross.sync.notifications.domain.entity.SyncedNotification

interface NotificationPublisher {
    suspend fun publish(notification: SyncedNotification): Result<Unit>
    suspend fun remove(removal: NotificationRemoval): Result<Unit>
    suspend fun publishSnapshot(snapshot: NotificationSnapshot): Result<Unit>
}
