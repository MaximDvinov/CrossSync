package com.cross.sync.syncing.domain.usecases

import com.cross.sync.notifications.domain.entity.NotificationActionRequest
import com.cross.sync.syncing.domain.repository.ClipboardServer

class SendNotificationActionUseCase(
    private val server: ClipboardServer,
) {
    suspend operator fun invoke(action: NotificationActionRequest): Result<Unit> {
        return server.sendNotificationAction(action)
    }
}
