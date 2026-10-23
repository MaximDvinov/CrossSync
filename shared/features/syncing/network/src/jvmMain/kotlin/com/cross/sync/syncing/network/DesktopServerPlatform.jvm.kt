package com.cross.sync.syncing.network

import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import java.net.BindException

internal actual fun createDesktopServer(scope: CoroutineScope, port: Int, configure: Application.() -> Unit): EmbeddedServer<*, *> =
    scope.embeddedServer(Netty, port = port, module = configure)

internal actual fun Throwable.isAddressAlreadyInUse(): Boolean =
    generateSequence(this) { it.cause }.any { it is BindException }

class JvmServerTokens : ServerTokens {
    override fun access(deviceId: String) = JwtConfig.generateAccessToken(deviceId)
    override fun refresh(deviceId: String) = JwtConfig.generateRefreshToken(deviceId)
    override fun verifyAccess(token: String) = runCatching {
        JwtConfig.verifier.verify(token).getClaim("deviceId").asString()
    }.getOrNull()
    override fun verifyRefresh(token: String) = JwtConfig.verifyRefreshToken(token)?.getClaim("deviceId")?.asString()
}
