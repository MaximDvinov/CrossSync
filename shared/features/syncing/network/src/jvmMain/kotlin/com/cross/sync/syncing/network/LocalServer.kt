package com.cross.sync.syncing.network

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerEvent
import com.cross.sync.syncing.domain.entity.ServerState
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.network.NetworkUtils.getIpAddress
import com.cross.sync.syncing.network.crypto.CryptoEngine
import com.cross.sync.syncing.network.model.ConnectionRespond
import com.cross.sync.syncing.network.model.CopiedDataDto
import com.cross.sync.syncing.network.model.DataPackage
import com.cross.sync.syncing.network.model.DeviceDataDto
import com.cross.sync.syncing.network.model.QrConnectionConfig
import com.cross.sync.syncing.network.model.RefreshRequest
import com.cross.sync.syncing.network.model.TokenResponse
import com.cross.sync.syncing.network.model.toDomain
import com.cross.sync.syncing.network.model.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Collections

class LocalServer(
    private val cryptoEngine: CryptoEngine,
    private val deviceRepository: DeviceRepository
) : ClipboardServer {
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json: Json = Json
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
        null
    private val connections =
        Collections.synchronizedMap(mutableMapOf<String, DefaultWebSocketServerSession>())

    private val serverState: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped())
    private val serverEvent: MutableStateFlow<ServerEvent?> = MutableStateFlow(null)
    private val pairingState: MutableStateFlow<PairingState> = MutableStateFlow(PairingState.Idle())

    private var actualQrCodeConfig: QrConnectionConfig? = null

    override fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent?>> {
        if (server != null) return serverState to serverEvent

        coroutineScope.launch {
            serverState.emit(ServerState.Starting())
        }
        server = embeddedServer(Netty, port = 33333) {
            converterInit()
            jsonInit(json)
            loggerInit()
            authInit()

            routing {
                routes()
                webSocketInit()
            }
        }.start(wait = false)

        server?.monitor?.subscribe(ApplicationStarted) {
            println("SERVER STARTED")
        }

        coroutineScope.launch {
            serverState.emit(ServerState.Started())
        }

        return serverState to serverEvent
    }

    private fun Application.authInit() {
        install(Authentication) {
            jwt(AUTH_NAME) {
                realm = JwtConfig.REALM
                verifier(JwtConfig.verifier)

                validate { credential ->
                    if (credential.payload.audience.contains(JwtConfig.AUDIENCE)) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }

                challenge { defaultScheme, realm ->
                    call.respond(HttpStatusCode.Unauthorized, "Token is invalid or expired")
                }
            }
        }
    }

    private fun Routing.routes() {
        post(PAIR_ROUTE) {
            println("Device pair")

            val deviceDataDto = call.receive<DeviceDataDto>()
            if (deviceDataDto.pairingKey == actualQrCodeConfig?.pairingKey) {
                val access = JwtConfig.generateAccessToken(deviceDataDto.id)
                val refresh = JwtConfig.generateRefreshToken(deviceDataDto.id)

                val device = DeviceData(
                    id = deviceDataDto.id,
                    name = deviceDataDto.name,
                    secretKey = actualQrCodeConfig!!.secretKey,
                    accessToken = access,
                    refreshToken = refresh
                )
                pairingState.emit(PairingState.Connected(device))

                serverEvent.emit(ServerEvent.AddedDevice(device))

                actualQrCodeConfig = null

                call.respond(status = HttpStatusCode.OK, TokenResponse(access, refresh))
            } else {
                call.respond(HttpStatusCode.BadRequest, ConnectionRespond(false))
            }
        }

        post(AUTH_REFRESH_ROUTE) {
            val req = call.receive<RefreshRequest>()
            val decoded = JwtConfig.verifyRefreshToken(req.refreshToken)
            val deviceId = decoded?.getClaim("deviceId")?.asString()

            if (deviceId != null) {
                val device = deviceRepository.getDeviceById(deviceId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    "Device not found"
                )
                if (device.refreshToken == req.refreshToken) {
                    val newAccess = JwtConfig.generateAccessToken(deviceId)
                    val newRefresh = JwtConfig.generateRefreshToken(deviceId)

                    deviceRepository.saveDevice(
                        device.copy(
                            accessToken = newAccess,
                            refreshToken = newRefresh
                        )
                    )
                    call.respond(TokenResponse(newAccess, newRefresh))
                    return@post
                }
            }
            call.respond(HttpStatusCode.Unauthorized, "Invalid Refresh Token")
        }

        authenticate(AUTH_NAME) {
            post(SEND_ROUTE) {
                val device = call.getAuthData()
                if (device == null) {
                    call.respond(HttpStatusCode.BadRequest, "Device incorrect")
                    return@post
                }

                val data = call.receive<DataPackage>()
                cryptoEngine.decrypt(data.encryptedContent, secretKey = device.secretKey)
                    .onSuccess {
                        val message = json.decodeFromString<CopiedDataDto>(it)
                        serverEvent.emit(ServerEvent.ReceivedCopiedData(message.toDomain()))
                    }

                call.respond(HttpStatusCode.OK)
            }
        }
    }

    private fun Routing.webSocketInit() {
        authenticate(AUTH_NAME) {
            webSocket(SYNC_ROUTE) {
                val device = call.getAuthData() ?: return@webSocket close(
                    CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No device")
                )

                connections[device.id] = this

                try {
                    incoming.consumeEach {
                        val data = receiveDeserialized<DataPackage>()
                        cryptoEngine.decrypt(data.encryptedContent, secretKey = device.secretKey)
                            .onSuccess {
                                val message = json.decodeFromString<CopiedDataDto>(it)
                                serverEvent.emit(ServerEvent.ReceivedCopiedData(message.toDomain()))
                            }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("onClose ${closeReason.await()}")
                } catch (e: Throwable) {
                    println("onError ${closeReason.await()}")
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun ApplicationCall.getAuthData(): DeviceData? {
        val principal = principal<JWTPrincipal>()
        val deviceId = principal?.payload?.getClaim("deviceId")?.asString()

        val device = deviceId?.let {
            deviceRepository.getDeviceById(it)
        }

        return device
    }

    override suspend fun sendCopiedDataWebSocket(message: CopiedData): Result<Unit> {
        connections.forEach { (deviceId, session) ->
            val json = json.encodeToString(message.toDto())
            val device = deviceId?.let { deviceRepository.getDeviceById(it) }
            val encryptedMessage =
                device?.secretKey?.let { cryptoEngine.encrypt(json, it) }


            if (encryptedMessage != null) {
                session.sendSerialized(DataPackage(encryptedMessage))
            }
        }

        return Result.success(Unit)
    }

    override fun observeServerState(): StateFlow<ServerState> {
        return serverState
    }

    override fun observeServerEvent(): SharedFlow<ServerEvent?> {
        return serverEvent
    }

    override fun observePairingState(): StateFlow<PairingState?> {
        return pairingState
    }

    override fun stop() {
        server?.stop(1000, 2000)
        coroutineScope.launch {
            serverState.emit(ServerState.Stopped())
        }
        server = null
    }

    override fun startPairing(): StateFlow<PairingState> {
        val qrConnectionConfig = generateQqCode()
        val jsonQrData = json.encodeToString(qrConnectionConfig)
        actualQrCodeConfig = qrConnectionConfig

        coroutineScope.launch {
            pairingState.emit(PairingState.QrCodeGenerated(jsonQrData))
        }

        return pairingState
    }


    fun generateQqCode(): QrConnectionConfig {
        val ipv6List = NetworkUtils.getLinkLocalIPv6Addresses()
        val myIp = ipv6List.firstOrNull { it.contains("en0") } ?: ipv6List.firstOrNull()
        val cleanIp = myIp?.substringBefore("%")
        return QrConnectionConfig(
            ip = cleanIp ?: getIpAddress(),
            port = 33333,
            secretKey = cryptoEngine.generateKey(),
            pairingKey = (10000..99999).random().toString()
        )
    }
}

private fun Application.loggerInit() {
    install(CallLogging) {
        level = Level.INFO

        filter { call -> call.request.path().startsWith("/") }
    }
}

private fun Application.jsonInit(json: Json) {
    install(ContentNegotiation) { json(json) }
}

private fun Application.converterInit() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
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

    fun getLinkLocalIPv6Addresses(): List<String> {
        val addresses = mutableListOf<String>()

        try {
            // Получаем все сетевые интерфейсы
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()

            for (intf in interfaces) {
                // Отфильтровываем:
                // 1. Неактивные (down)
                // 2. Петлевые (localhost/127.0.0.1)
                // 3. Виртуальные (обычно они нам не нужны, но для VPN иногда приходится фильтровать жестче)
                if (!intf.isUp || intf.isLoopback) continue

                // Перебираем IP адреса на этом интерфейсе
                val inetAddresses = intf.inetAddresses.toList()

                for (addr in inetAddresses) {
                    // Нам нужен только IPv6 и только Link-Local (fe80::...)
                    if (addr is Inet6Address && addr.isLinkLocalAddress) {

                        // addr.hostAddress может вернуть что-то типа "fe80:0:0:0:abcd:1234:5678:ef90%en0"
                        // %en0 — это Scope ID (имя интерфейса), оно важно!
                        val fullAddress = addr.hostAddress

                        // Иногда в адресе есть лишние данные, чистим если нужно,
                        // но для Link-Local Scope ID (%en0) часто обязателен.
                        addresses.add(fullAddress)

                        println("Found Interface: ${intf.displayName} -> $fullAddress")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return addresses
    }
}
