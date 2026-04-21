package com.cross.sync.syncing.domain.entity

data class DeviceData(
    val id: String,
    val name: String,
    val secretKey: String,
    val accessToken: String,
    val refreshToken: String
)

class DeviceNotInitializedException : Exception()