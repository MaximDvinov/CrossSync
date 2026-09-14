package com.cross.sync.syncing.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.syncing.domain.entity.ClientConnectState
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun pairToServer(
        deviceId: String,
        deviceName: String,
        qrConnectionConfig: String
    ): Result<Unit>

    suspend fun connect(): Result<Flow<ClientConnectState>>

    suspend fun sendCopiedData(data: CopiedData): Result<Unit>
    suspend fun sendNotification(notification: SyncedNotification): Result<Unit>
    suspend fun removeNotification(removal: NotificationRemoval): Result<Unit>

    fun disconnect(): Result<Unit>
}
