package com.cross.sync.syncing.network.model

import kotlinx.serialization.Serializable

@Serializable
data class QrConnectionConfig(
    val ip: String,
    val port: Int,
    val secretKey: String,
    val pairingKey: String
)

@Serializable
data class DataPackage(
    val encryptedContent: String
)

@Serializable
data class DeviceDataDto(
    val id: String,
    val name: String,
    val pairingKey: String
)


@Serializable
data class ConnectionRespond(
    val isConnection: Boolean
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val serverName: String? = null,
    val serverIp: String? = null
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)
