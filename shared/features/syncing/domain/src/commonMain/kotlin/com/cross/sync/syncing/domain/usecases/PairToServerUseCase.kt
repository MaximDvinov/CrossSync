package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.repository.SyncRepository

class PairToServerUseCase(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        deviceName: String,
        qrConnectionConfig: String
    ): Result<Unit> {
        return syncRepository.pairToServer(deviceId, deviceName, qrConnectionConfig)
    }
}
