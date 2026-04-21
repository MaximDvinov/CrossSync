package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow

class ObserveDevicesUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): Flow<List<DeviceData>> {
        return deviceRepository.observeDevices()
    }
}