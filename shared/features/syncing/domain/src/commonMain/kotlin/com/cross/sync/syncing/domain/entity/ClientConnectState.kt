package com.cross.sync.syncing.domain.entity

sealed interface ClientConnectState {
    data object Idle : ClientConnectState
    data object Connecting : ClientConnectState
    data object Connected : ClientConnectState
    data class Disconnected(val cause: Throwable?) : ClientConnectState
    data class Error(val message: String) : ClientConnectState
}