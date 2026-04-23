package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.repository.DeviceRepository

class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(id: String) {
        deviceRepository.deleteDevice(id)
    }
}
