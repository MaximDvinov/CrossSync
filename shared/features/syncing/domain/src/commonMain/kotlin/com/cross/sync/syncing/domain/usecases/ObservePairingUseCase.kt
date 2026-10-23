package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.repository.ClipboardServer
import kotlinx.coroutines.flow.StateFlow

class ObservePairingUseCase(
    private val server: ClipboardServer,
) {
    operator fun invoke(): StateFlow<PairingState?>  {
        return server.observePairingState()
    }
}