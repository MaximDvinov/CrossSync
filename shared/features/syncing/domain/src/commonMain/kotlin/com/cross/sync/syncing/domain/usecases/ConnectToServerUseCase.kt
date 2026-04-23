package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.domain.entity.DeviceNotInitializedException
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow

class ConnectToServerUseCase(
    private val syncRepository: SyncRepository,
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): Result<Flow<ClientConnectState>> {
        deviceRepository.getDeviceById(null) ?: return Result.failure(
            DeviceNotInitializedException()
        )
        return syncRepository.connect()
    }
}