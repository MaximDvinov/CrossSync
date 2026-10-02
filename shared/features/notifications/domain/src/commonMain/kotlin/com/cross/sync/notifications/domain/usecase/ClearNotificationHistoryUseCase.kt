package com.cross.sync.notifications.domain.usecase

import com.cross.sync.notifications.domain.repository.NotificationRepository

class ClearNotificationHistoryUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke() {
        repository.clear()
    }

    suspend fun before(olderThanEpochMillis: Long) {
        repository.clearHistoryBefore(olderThanEpochMillis)
    }
}
