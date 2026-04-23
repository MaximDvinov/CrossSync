package com.cross.sync.core.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
class DeviceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val secretKey: String,
    val accessToken: String,
    val refreshToken: String
)