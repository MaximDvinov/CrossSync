package com.cross.sync.notifications.domain.usecase

import com.cross.sync.notifications.domain.repository.NotificationRepository

class MarkNotificationReadUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(deviceId: String, notificationKey: String) {
        repository.markRead(deviceId, notificationKey)
    }
}
