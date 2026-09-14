package com.cross.sync.syncing.network

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.notifications.domain.entity.NotificationActionRequest
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
import com.cross.sync.syncing.network.model.SyncPayloadDto
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class LocalServer(
    private val cryptoEngine: CryptoEngine,
    private val deviceRepository: DeviceRepository,
    private val localClipboardRepository: LocalClipboardRepository,
    private val systemClipboardRepository: SystemClipboardRepository
) : ClipboardServer {
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json: Json = Json
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
        null
    private val connections =
        Collections.synchronizedMap(mutableMapOf<String, DefaultWebSocketServerSession>())

    private val serverState: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped())
    private val serverEvent = MutableSharedFlow<ServerEvent>(
        replay = 0,
        extraBufferCapacity = SERVER_EVENT_BUFFER_SIZE
    )
    private val pairingState: MutableStateFlow<PairingState> = MutableStateFlow(PairingState.Idle())

    private var actualQrCodeConfig: QrConnectionConfig? = null
    private var activeQrCodeJson: String? = null

    override fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent>> {
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
                emitPairingConnected(device)

                serverEvent.emit(ServerEvent.AddedDevice(device))

                val pairedServerIp = actualQrCodeConfig?.ip ?: getIpAddress()
                val pairedServerIps = actualQrCodeConfig?.ipAddresses.orEmpty()
                actualQrCodeConfig = null
                activeQrCodeJson = null

                call.respond(
                    status = HttpStatusCode.OK,
                    message = TokenResponse(
                        accessToken = access,
                        refreshToken = refresh,
                        serverName = resolveServerName(),
                        serverIp = pairedServerIp,
                        serverIps = pairedServerIps
                    )
                )
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
                val decryptedData = cryptoEngine.decrypt(
                    data.encryptedContent,
                    secretKey = device.secretKey
                ).getOrElse {
                    call.respond(HttpStatusCode.BadRequest, "Cannot decrypt sync data")
                    return@post
                }
                val payload = runCatching {
                    val payload = json.decodeFromString<SyncPayloadDto>(decryptedData)
                    payload
                }.getOrNull()

                when (payload) {
                    is SyncPayloadDto.CopiedDataPayload -> {
                        serverEvent.emit(ServerEvent.ReceivedCopiedData(payload.data.toDomain()))
                    }

                    is SyncPayloadDto.NotificationPayload -> {
                        serverEvent.emit(
                            ServerEvent.ReceivedNotification(
                                deviceId = device.id,
                                notification = payload.notification.copy(deviceId = device.id),
                            )
                        )
                    }

                    is SyncPayloadDto.NotificationRemovedPayload -> {
                        serverEvent.emit(
                            ServerEvent.RemovedNotification(
                                deviceId = device.id,
                                removal = payload.removal,
                            )
                        )
                    }

                    null -> runCatching {
                        val copiedData = json.decodeFromString<CopiedDataDto>(decryptedData).toDomain()
                        serverEvent.emit(ServerEvent.ReceivedCopiedData(copiedData))
                    }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest, "Invalid sync payload")
                        return@post
                    }

                    else -> {
                        call.respond(HttpStatusCode.BadRequest, "Unsupported sync payload")
                        return@post
                    }
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
                emitPairingConnected(device)

                try {
                    val snapshotRequest = receiveDeserialized<DataPackage>()
                    val knownCopiedDataIds = cryptoEngine.decrypt(
                        snapshotRequest.encryptedContent,
                        secretKey = device.secretKey
                    ).mapCatching { decrypted ->
                        json.decodeFromString<SyncPayloadDto>(decrypted)
                    }.getOrNull()
                        ?.let { it as? SyncPayloadDto.SnapshotRequestPayload }
                        ?.knownCopiedDataIds
                        .orEmpty()

                    sendSnapshot(device, this, knownCopiedDataIds)

                    incoming.consumeEach {
                        val data = receiveDeserialized<DataPackage>()
                        cryptoEngine.decrypt(data.encryptedContent, secretKey = device.secretKey)
                            .onSuccess {
                                val payload = runCatching {
                                    json.decodeFromString<SyncPayloadDto>(it)
                                }.getOrNull()

                                val copiedData = when (payload) {
                                    is SyncPayloadDto.CopiedDataPayload -> payload.data.toDomain()
                                    else -> json.decodeFromString<CopiedDataDto>(it).toDomain()
                                }
                                serverEvent.emit(ServerEvent.ReceivedCopiedData(copiedData))
                            }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("onClose ${closeReason.await()}")
                } catch (e: Throwable) {
                    println("onError ${closeReason.await()}")
                    e.printStackTrace()
                } finally {
                    connections.remove(device.id)
                    emitPairingByActiveConnections()
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
        if (!message.isSyncableText()) return Result.success(Unit)

        val activeConnections = synchronized(connections) { connections.toMap() }
        var firstError: Throwable? = null

        activeConnections.forEach { (deviceId, session) ->
            val payload = SyncPayloadDto.CopiedDataPayload(message.toDto())
            val json = json.encodeToString<SyncPayloadDto>(payload)
            val device = deviceId?.let { deviceRepository.getDeviceById(it) }
            val encryptedMessage =
                device?.secretKey?.let { cryptoEngine.encrypt(json, it) }

            if (encryptedMessage != null) {
                runCatching {
                    session.sendSerialized(DataPackage(encryptedMessage))
                }.onFailure { error ->
                    if (firstError == null) firstError = error
                    synchronized(connections) {
                        if (connections[deviceId] === session) {
                            connections.remove(deviceId)
                        }
                    }
                }
            }
        }

        return firstError?.let(Result.Companion::failure) ?: Result.success(Unit)
    }

    override suspend fun sendNotificationAction(action: NotificationActionRequest): Result<Unit> {
        val session = synchronized(connections) { connections[action.deviceId] }
            ?: return Result.failure(IllegalStateException("Android device is offline"))
        val device = deviceRepository.getDeviceById(action.deviceId)
            ?: return Result.failure(IllegalArgumentException("Unknown device"))

        return runCatching {
            sendPayload(
                session = session,
                secretKey = device.secretKey,
                payload = SyncPayloadDto.NotificationActionPayload(action),
            )
        }.onFailure {
            synchronized(connections) {
                if (connections[action.deviceId] === session) connections.remove(action.deviceId)
            }
        }
    }

    private suspend fun sendSnapshot(
        device: DeviceData,
        session: DefaultWebSocketServerSession,
        knownCopiedDataIds: Set<Long>
    ) {
        val categories = localClipboardRepository.observeCategories().first()
        categories.forEach { category ->
            sendPayload(
                session = session,
                secretKey = device.secretKey,
                payload = SyncPayloadDto.CategoryPayload(
                    categoryId = category.id,
                    name = category.name
                )
            )
        }

        val allCopiedData = localClipboardRepository.getAllCopiedData()
            .filter { it.isSyncableText() }

        val copiedData = allCopiedData
            .filterNot { it.id in knownCopiedDataIds }
            .sortedBy { it.dateTime }

        copiedData.forEach { data ->
            sendPayload(
                session = session,
                secretKey = device.secretKey,
                payload = SyncPayloadDto.HistoryCopiedDataPayload(data.toDto())
            )
        }

        // Bindings are idempotent and must also be sent for history the client already has.
        val syncedIds = allCopiedData.mapTo(mutableSetOf()) { it.id }
        categories.forEach { category ->
            localClipboardRepository.observeCopiedDataByCategory(category.id).first()
                .filter { it.id in syncedIds }
                .forEach { data ->
                    sendPayload(
                        session = session,
                        secretKey = device.secretKey,
                        payload = SyncPayloadDto.CategoryBindingPayload(
                            categoryId = category.id,
                            copiedDataId = data.id
                        )
                    )
                }
        }

        systemClipboardRepository.getData()
            ?.takeIf { it.isSyncableText() }
            ?.let { currentClipboard ->
                sendPayload(
                    session = session,
                    secretKey = device.secretKey,
                    payload = SyncPayloadDto.CurrentClipboardPayload(currentClipboard.toDto())
                )
            }
    }

    private suspend fun sendPayload(
        session: DefaultWebSocketServerSession,
        secretKey: String,
        payload: SyncPayloadDto
    ) {
        val plain = json.encodeToString<SyncPayloadDto>(payload)
        val encrypted = cryptoEngine.encrypt(plain, secretKey)
        session.sendSerialized(DataPackage(encrypted))
    }

    override fun observeServerState(): StateFlow<ServerState> {
        return serverState
    }

    override fun observeServerEvent(): SharedFlow<ServerEvent> {
        return serverEvent
    }

    override fun observePairingState(): StateFlow<PairingState?> {
        return pairingState
    }

    override fun stop() {
        server?.stop(1000, 2000)
        connections.clear()
        activeQrCodeJson = null
        actualQrCodeConfig = null
        coroutineScope.launch {
            serverState.emit(ServerState.Stopped())
            pairingState.emit(PairingState.Idle())
        }
        server = null
    }

    override fun startPairing(): StateFlow<PairingState> {
        val qrConnectionConfig = generateQqCode()
        val jsonQrData = json.encodeToString(qrConnectionConfig)
        actualQrCodeConfig = qrConnectionConfig
        activeQrCodeJson = jsonQrData

        coroutineScope.launch {
            pairingState.emit(PairingState.QrCodeGenerated(jsonQrData))
        }

        return pairingState
    }


    fun generateQqCode(): QrConnectionConfig {
        val lanAddresses = NetworkUtils.getLanIpAddresses()
        val primaryIp = lanAddresses.firstOrNull() ?: getIpAddress()

        return QrConnectionConfig(
            ip = primaryIp,
            port = 33333,
            secretKey = cryptoEngine.generateKey(),
            pairingKey = (10000..99999).random().toString(),
            ipAddresses = lanAddresses
        )
    }

    private suspend fun emitPairingConnected(device: DeviceData) {
        pairingState.emit(PairingState.Connected(device))
    }

    private suspend fun emitPairingByActiveConnections() {
        val firstConnectedDeviceId = connections.keys.firstOrNull()
        if (firstConnectedDeviceId != null) {
            val device = deviceRepository.getDeviceById(firstConnectedDeviceId)
            if (device != null) {
                pairingState.emit(PairingState.Connected(device))
                return
            }
        }

        val activeQr = activeQrCodeJson
        if (activeQr != null) {
            pairingState.emit(PairingState.QrCodeGenerated(activeQr))
        } else {
            pairingState.emit(PairingState.Idle())
        }
    }

    private companion object {
        const val SERVER_EVENT_BUFFER_SIZE = 64
    }
}

private fun resolveServerName(): String {
    val localHostName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
    return localHostName
        ?.substringBefore('.')
        ?.takeIf { it.isNotBlank() }
        ?: "Mac"
}

private fun CopiedData.isSyncableText(): Boolean {
    return this is CopiedData.Text || this is CopiedData.FormattedText
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
        return getLanIpAddresses().firstOrNull() ?: "127.0.0.1"
    }

    fun getLanIpAddresses(): List<String> {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .asSequence()
                .filter { it.isUsableLanInterface() }
                .sortedWith(compareByDescending<NetworkInterface> { it.isPreferredLanInterface() }
                    .thenBy { it.index })
                .flatMap { iface ->
                    iface.inetAddresses.toList().asSequence()
                        .filterIsInstance<Inet4Address>()
                        .filter { it.isUsableLanAddress() }
                        .map { it.hostAddress }
                }
                .distinct()
                .toList()
        }.getOrDefault(emptyList())
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

    private fun NetworkInterface.isUsableLanInterface(): Boolean {
        if (!isUp || isLoopback || isPointToPoint || isVirtual) return false

        val id = listOf(name, displayName)
            .joinToString(" ")
            .lowercase()

        val blockedNames = listOf(
            "utun",
            "tun",
            "tap",
            "ppp",
            "ipsec",
            "wg",
            "wireguard",
            "tailscale",
            "zerotier",
            "zt",
            "docker",
            "bridge",
            "vbox",
            "vmnet",
            "awdl",
            "llw"
        )

        return blockedNames.none { it in id }
    }

    private fun NetworkInterface.isPreferredLanInterface(): Boolean {
        val id = listOf(name, displayName)
            .joinToString(" ")
            .lowercase()

        return listOf("en0", "en1", "wi-fi", "wifi", "wlan", "ethernet", "eth").any { it in id }
    }

    private fun Inet4Address.isUsableLanAddress(): Boolean {
        if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isMulticastAddress) {
            return false
        }

        val bytes = address.map { it.toInt() and 0xff }
        val first = bytes[0]
        val second = bytes[1]

        return first == 10 ||
            first == 192 && second == 168 ||
            first == 172 && second in 16..31
    }
}
