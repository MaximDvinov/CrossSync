package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.cross.sync.core.db.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    // Pairing and token refresh must replace credentials for an existing device.
    @Insert(onConflict = REPLACE)
    suspend fun insert(deviceEntity: DeviceEntity)

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getById(id: String): DeviceEntity?

    @Query("SELECT * FROM devices LIMIT 1")
    suspend fun getLastDevice(): DeviceEntity?

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM devices")
    suspend fun deleteAll()

    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>
}
