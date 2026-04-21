package com.cross.sync.syncing.domain.entity

sealed interface PairingState {
    class Idle : PairingState
    data class QrCodeGenerated(val qrCode: String): PairingState
    data class Connected(val deviceData: DeviceData): PairingState
    data class Error(val message: String, val throwable: Throwable): PairingState
}