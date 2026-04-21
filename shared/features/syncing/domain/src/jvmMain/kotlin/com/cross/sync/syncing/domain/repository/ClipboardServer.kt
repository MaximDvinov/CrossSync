package com.cross.sync.syncing.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerEvent
import com.cross.sync.syncing.domain.entity.ServerState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ClipboardServer {
    fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent?>>
    fun stop()

    fun startPairing(): StateFlow<PairingState>
    suspend fun sendCopiedDataWebSocket(message: CopiedData): Result<Unit>
    fun observeServerState(): StateFlow<ServerState>
    fun observeServerEvent(): SharedFlow<ServerEvent?>
    fun observePairingState(): StateFlow<PairingState?>
}