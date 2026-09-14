package com.cross.sync.notifications.domain.usecase

import com.cross.sync.notifications.domain.repository.NotificationRepository

class ObserveNotificationsUseCase(
    private val repository: NotificationRepository,
) {
    operator fun invoke() = repository.observeNotifications()
}
