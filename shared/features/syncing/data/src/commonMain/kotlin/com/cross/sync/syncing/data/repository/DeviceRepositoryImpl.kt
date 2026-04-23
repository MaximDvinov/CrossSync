package com.cross.sync.syncing.data.repository

import com.cross.sync.core.db.DeviceDao
import com.cross.sync.core.db.entities.DeviceEntity
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeviceRepositoryImpl(
    private val deviceDao: DeviceDao
) : DeviceRepository {
    override suspend fun saveDevice(deviceData: DeviceData) {
        deviceDao.insert(deviceEntity = deviceData.toEntity())
    }

    override suspend fun getDeviceById(id: String?): DeviceData? {
        val device = if (id == null) deviceDao.getLastDevice() else deviceDao.getById(id)
        return device?.toData()
    }

    override suspend fun deleteDevice(id: String) {
        deviceDao.deleteById(id)
    }

    override fun observeDevices(): Flow<List<DeviceData>> {
        return deviceDao.getAllDevices().map {
            it.map { entity -> entity.toData() }
        }
    }
}


// TODO: перенести в мапперы
fun DeviceData.toEntity() = DeviceEntity(
    id = id,
    name = name,
    secretKey = secretKey,
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun DeviceEntity.toData() = DeviceData(
    id = id,
    name = name,
    secretKey = secretKey,
    accessToken = accessToken,
    refreshToken = refreshToken
)
