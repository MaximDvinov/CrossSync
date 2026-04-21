package com.cross.sync.syncing.domain.repository

import com.cross.sync.syncing.domain.entity.DeviceData
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun saveDevice(deviceData: DeviceData)
    suspend fun getDeviceById(id: String?): DeviceData?
    fun observeDevices(): Flow<List<DeviceData>>
}