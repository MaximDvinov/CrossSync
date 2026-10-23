package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.domain.repository.ClipboardServer

class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository,
    private val server: ClipboardServer,
) {
    suspend operator fun invoke(id: String) {
        deviceRepository.deleteDevice(id)
        server.disconnectDevice(id)
    }
}
