package com.cross.sync.syncing.network

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineScope
import io.ktor.utils.io.errors.PosixException

internal actual fun createDesktopServer(scope: CoroutineScope, port: Int, configure: Application.() -> Unit): EmbeddedServer<*, *> =
    scope.embeddedServer(CIO, port = port, module = configure).apply {
        // Rebind after our previous TCP sessions enter TIME_WAIT; this is not SO_REUSEPORT.
        engineConfig.reuseAddress = true
    }

internal actual fun Throwable.isAddressAlreadyInUse(): Boolean =
    generateSequence(this) { it.cause }.any { it is PosixException.AddressAlreadyInUseException }
