package com.cross.sync.syncing.network

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.Frame.*
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import jdk.internal.net.http.common.Utils.close
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

import org.slf4j.event.*
import io.ktor.server.request.*
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.serialization.sendSerializedBase
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow

class LocalServer() {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
        null
    private val connections =
        Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())
    private val messages: MutableStateFlow<ClipboardMessageDto?> =
        MutableStateFlow(null)

    fun start() {
        if (server != null) return

        println("Starting server...")
        server = embeddedServer(Netty, port = 33333, host = "0.0.0.0") {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            install(ContentNegotiation) { json() }
            install(CallLogging) {
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/") }
            }

            routing {
                get("/info") {
                    val ip = NetworkUtils.getIpAddress()
                    call.respond(
                        mapOf(
                            "host" to ip,
                            "port" to "33333",
                            "deviceId" to "MAC-${UUID.randomUUID()}"
                        )
                    )
                }

                webSocket("/sync") {
                    connections.add(this)

                    try {
                        incoming.consumeEach{
                            messages.value = receiveDeserialized<ClipboardMessageDto>()
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        println("onClose ${closeReason.await()}")
                    } catch (e: Throwable) {
                        println("onError ${closeReason.await()}")
                        e.printStackTrace()
                    }
                }
            }
        }.start(wait = true)
    }

    fun observeMessages(): StateFlow<ClipboardMessageDto?> {
        return messages
    }

    suspend fun broadcast(message: ClipboardMessageDto): Result<Unit> {
        println("Broadcasting message: $message")
        connections.forEach { session ->
            session.sendSerialized(message)
        }

        return Result.success(Unit)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}

object NetworkUtils {
    fun getIpAddress(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in interfaces) {
            val addrs = iface.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        return "127.0.0.1"
    }
}