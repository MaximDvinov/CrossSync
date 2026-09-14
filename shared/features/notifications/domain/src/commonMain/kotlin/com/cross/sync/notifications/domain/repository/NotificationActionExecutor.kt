package com.cross.sync.notifications.domain.repository

import com.cross.sync.notifications.domain.entity.NotificationActionRequest

interface NotificationActionExecutor {
    suspend fun execute(request: NotificationActionRequest): Result<Unit>
}
