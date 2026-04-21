package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.repository.ClipboardServer
import kotlinx.coroutines.flow.StateFlow

class AddDeviceUseCase(
    private val server: ClipboardServer
) {
    suspend operator fun invoke(): StateFlow<PairingState> {
        return server.startPairing()
    }
}