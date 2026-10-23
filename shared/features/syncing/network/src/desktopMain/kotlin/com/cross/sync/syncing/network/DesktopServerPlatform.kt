package com.cross.sync.syncing.network

import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.CoroutineScope

internal expect fun createDesktopServer(scope: CoroutineScope, port: Int, configure: Application.() -> Unit): EmbeddedServer<*, *>

internal expect fun Throwable.isAddressAlreadyInUse(): Boolean

interface ServerTokens {
    fun access(deviceId: String): String
    fun refresh(deviceId: String): String
    fun verifyAccess(token: String): String?
    fun verifyRefresh(token: String): String?
}

interface ServerNetworkInfo {
    fun addresses(): List<String>
    fun name(): String
}
