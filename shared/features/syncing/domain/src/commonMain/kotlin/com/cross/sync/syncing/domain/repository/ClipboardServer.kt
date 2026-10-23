package com.cross.sync.syncing.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerEvent
import com.cross.sync.syncing.domain.entity.ServerState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.cross.sync.notifications.domain.entity.NotificationActionRequest

interface ClipboardServer {
    fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent>>
    fun stop()
    suspend fun stopAndJoin() { stop() }

    fun cancelPairing() {}
    suspend fun disconnectDevice(id: String) {}
    fun startPairing(): StateFlow<PairingState>
    suspend fun sendCopiedDataWebSocket(message: CopiedData): Result<Unit>
    suspend fun sendNotificationAction(action: NotificationActionRequest): Result<Unit>
    fun observeServerState(): StateFlow<ServerState>
    fun observeServerEvent(): SharedFlow<ServerEvent>
    fun observePairingState(): StateFlow<PairingState?>
}
