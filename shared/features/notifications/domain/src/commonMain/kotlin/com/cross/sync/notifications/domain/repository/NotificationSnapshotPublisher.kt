package com.cross.sync.notifications.domain.repository

interface NotificationSnapshotPublisher {
    suspend fun publishActive(force: Boolean = true): Result<Unit>
}
