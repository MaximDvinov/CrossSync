package com.cross.sync.syncing.data.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.domain.repository.SyncRepository
import com.cross.sync.syncing.network.ClipboardClient
import kotlinx.coroutines.flow.Flow

open class SyncRepositoryImpl(
    private val client: ClipboardClient
) : SyncRepository {
    override suspend fun pairToServer(
        deviceId: String,
        deviceName: String,
        qrConnectionConfig: String
    ): Result<Unit> {
        return client.pairToServer(
            deviceId = deviceId,
            deviceName = deviceName,
            qrConnectionConfig = qrConnectionConfig
        )
    }

    override suspend fun connect(): Result<Flow<ClientConnectState>> {
        return Result.success(client.connect())
    }

    override suspend fun sendCopiedData(data: CopiedData): Result<Unit> {
        return client.sendCopiedData(data)
    }

    override suspend fun sendNotification(notification: SyncedNotification): Result<Unit> {
        return client.sendNotification(notification)
    }

    override suspend fun removeNotification(removal: NotificationRemoval): Result<Unit> {
        return client.removeNotification(removal)
    }

    override fun disconnect(): Result<Unit> = runCatching {
        client.disconnect()
    }
}
