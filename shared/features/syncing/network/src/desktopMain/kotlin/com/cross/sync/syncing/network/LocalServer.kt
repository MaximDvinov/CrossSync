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
import io.ktor.utils.io.ClosedByteChannelException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.russhwolf.settings.Settings

class LocalServer(
    private val cryptoEngine: CryptoEngine,
    private val deviceRepository: DeviceRepository,
    private val localClipboardRepository: LocalClipboardRepository,
    private val systemClipboardRepository: SystemClipboardRepository,
    private val tokens: ServerTokens,
    private val network: ServerNetworkInfo,
    private val port: Int = DEFAULT_PORT,
    private val settings: Settings? = null,
) : ClipboardServer {
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            // CIO also reports a failed bind from its engine coroutine; start() handles the retry.
            if (!error.isAddressAlreadyInUse()) {
                serverState.value = ServerState.Error("Не удалось запустить синхронизацию.", error)
            }
        })

    private val json: Json = Json
    private var server: EmbeddedServer<*, *>? =
        null
    private val connections = mutableMapOf<String, DefaultWebSocketServerSession>()
    private val connectionsMutex = Mutex()
    private val lifecycleMutex = Mutex()
    private val requestedStart = MutableStateFlow(false)
    private var startJob: Job? = null
    private var stopJob: Job? = null
    private var pairingJob: Job? = null
    private var boundPort: Int? = null

    private val serverState: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped())
    private val serverEvent = MutableSharedFlow<ServerEvent>(
        replay = 0,
        extraBufferCapacity = SERVER_EVENT_BUFFER_SIZE
    )
    private val pairingState: MutableStateFlow<PairingState> = MutableStateFlow(PairingState.Idle())

    private var actualQrCodeConfig: QrConnectionConfig? = null
    private var activeQrCodeJson: String? = null

    override fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent>> {
        if (!requestedStart.compareAndSet(false, true)) return serverState to serverEvent
        serverState.value = ServerState.Starting()
        startJob = coroutineScope.launch {
            lifecycleMutex.withLock {
                try {
                    val preferredPort = if (port == DEFAULT_PORT) {
                        settings?.getIntOrNull(SERVER_PORT_KEY)?.takeIf { it in 1..65535 } ?: port
                    } else {
                        port
                    }
                    try {
                        startServer(preferredPort)
                    } catch (error: Exception) {
                        // CIO can wrap a bind failure in CancellationException without cancelling this job.
                        currentCoroutineContext().ensureActive()
                        if (preferredPort == 0 || !error.isAddressAlreadyInUse()) {
                            throw error
                        }
                        server?.stopSuspend(0, 1000)
                        server = null
                        // Binding port 0 lets the OS select and reserve a free port atomically.
                        startServer(0)
                    }
                    settings?.putInt(SERVER_PORT_KEY, checkNotNull(boundPort))
                    serverState.emit(ServerState.Started())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    server?.stopSuspend(0, 1000)
                    server = null
                    boundPort = null
                    serverState.emit(ServerState.Error("Не удалось запустить синхронизацию.", error))
                    requestedStart.value = false
                }
            }
        }

        return serverState to serverEvent
    }

    private suspend fun startServer(listenPort: Int) {
        val instance = createDesktopServer(coroutineScope, listenPort) {
            converterInit()
            jsonInit(json)
            authInit()
            routing { routes(); webSocketInit() }
        }
        server = instance
        instance.startSuspend(wait = false)
        boundPort = instance.engine.resolvedConnectors().single().port
    }

    private fun Application.authInit() {
        install(Authentication) {
            bearer(AUTH_NAME) {
                authenticate { credential ->
                    tokens.verifyAccess(credential.token)?.let(::UserIdPrincipal)
                }
            }
        }
    }

    private fun Routing.routes() {
        post(PAIR_ROUTE) {
            println("Device pair")

            val deviceDataDto = call.receive<DeviceDataDto>()
            if (deviceDataDto.pairingKey == actualQrCodeConfig?.pairingKey) {
                val access = tokens.access(deviceDataDto.id)
                val refresh = tokens.refresh(deviceDataDto.id)

                val device = DeviceData(
                    id = deviceDataDto.id,
                    name = deviceDataDto.name,
                    secretKey = actualQrCodeConfig!!.secretKey,
                    accessToken = access,
                    refreshToken = refresh
                )
                deviceRepository.saveDevice(device)
                emitPairingConnected(device)

                serverEvent.emit(ServerEvent.AddedDevice(device))

                val pairedServerIp = actualQrCodeConfig?.ip ?: network.addresses().firstOrNull() ?: "127.0.0.1"
                val pairedServerIps = actualQrCodeConfig?.ipAddresses.orEmpty()
                actualQrCodeConfig = null
                activeQrCodeJson = null

                call.respond(
                    status = HttpStatusCode.OK,
                    message = TokenResponse(
                        accessToken = access,
                        refreshToken = refresh,
                        serverName = network.name(),
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
            val deviceId = tokens.verifyRefresh(req.refreshToken)

            if (deviceId != null) {
                val device = deviceRepository.getDeviceById(deviceId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    "Device not found"
                )
                if (device.refreshToken == req.refreshToken) {
                    val newAccess = tokens.access(deviceId)
                    val newRefresh = tokens.refresh(deviceId)

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

                    is SyncPayloadDto.NotificationSnapshotPayload -> {
                        serverEvent.emit(
                            ServerEvent.ReceivedNotificationSnapshot(device.id, payload.snapshot)
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

                connectionsMutex.withLock { connections[device.id] = this }
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

                    while (true) {
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
                } catch (_: ClosedByteChannelException) {
                    // A disconnected peer is handled by the session cleanup below.
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("WebSocket error")
                    e.printStackTrace()
                } finally {
                    connectionsMutex.withLock {
                        // A reconnect may already have replaced this session.
                        if (connections[device.id] === this) connections.remove(device.id)
                    }
                    emitPairingByActiveConnections()
                }
            }
        }
    }

    private suspend fun ApplicationCall.getAuthData(): DeviceData? {
        val deviceId = principal<UserIdPrincipal>()?.name

        val device = deviceId?.let {
            deviceRepository.getDeviceById(it)
        }

        return device
    }

    override suspend fun sendCopiedDataWebSocket(message: CopiedData): Result<Unit> {
        if (!message.isSyncableText()) return Result.success(Unit)

        val activeConnections = connectionsMutex.withLock { connections.toMap() }
        var firstError: Throwable? = null

        activeConnections.forEach { (deviceId, session) ->
            val payload = SyncPayloadDto.CopiedDataPayload(message.toDto())
            val json = json.encodeToString<SyncPayloadDto>(payload)
            val device = deviceRepository.getDeviceById(deviceId)
            val encryptedMessage =
                device?.secretKey?.let { cryptoEngine.encrypt(json, it) }

            if (encryptedMessage != null) {
                runCatching {
                    session.sendSerialized(DataPackage(encryptedMessage))
                }.onFailure { error ->
                    if (firstError == null) firstError = error
                    connectionsMutex.withLock {
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
        val session = connectionsMutex.withLock { connections[action.deviceId] }
            ?: return Result.failure(IllegalStateException("Android device is offline"))
        val device = deviceRepository.getDeviceById(action.deviceId)
            ?: return Result.failure(IllegalArgumentException("Unknown device"))

        return try {
            sendPayload(
                session = session,
                secretKey = device.secretKey,
                payload = SyncPayloadDto.NotificationActionPayload(action),
            )
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            connectionsMutex.withLock {
                if (connections[action.deviceId] === session) connections.remove(action.deviceId)
            }
            emitPairingByActiveConnections()
            Result.failure(error)
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
        startJob?.cancel()
        cancelPairing()
        stopJob = coroutineScope.launch {
            lifecycleMutex.withLock {
                server?.stopSuspend(0, 1000)
                server = null
                boundPort = null
                connectionsMutex.withLock { connections.clear() }
                serverState.value = ServerState.Stopped()
                pairingState.value = PairingState.Idle()
                requestedStart.value = false
            }
        }
    }

    override suspend fun stopAndJoin() {
        stop()
        stopJob?.join()
        coroutineScope.cancel()
        coroutineScope.coroutineContext[Job]?.join()
    }

    override fun cancelPairing() {
        pairingJob?.cancel()
        activeQrCodeJson = null
        actualQrCodeConfig = null
        coroutineScope.launch { emitPairingByActiveConnections() }
    }

    override suspend fun disconnectDevice(id: String) {
        val session = connectionsMutex.withLock { connections.remove(id) }
        session?.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Device unpaired"))
        emitPairingByActiveConnections()
    }

    override fun startPairing(): StateFlow<PairingState> {
        if (serverState.value is ServerState.Stopped) start()
        pairingJob?.cancel()
        pairingJob = coroutineScope.launch {
            val state = serverState.first { it !is ServerState.Starting }
            if (state is ServerState.Error) {
                pairingState.emit(PairingState.Error(state.message, state.throwable))
                return@launch
            }
            if (state !is ServerState.Started) {
                pairingState.emit(PairingState.Error("Сервер синхронизации не запущен.", IllegalStateException("Server is stopped")))
                return@launch
            }
            val qrConnectionConfig = generateQqCode()
            val jsonQrData = json.encodeToString(qrConnectionConfig)
            actualQrCodeConfig = qrConnectionConfig
            activeQrCodeJson = jsonQrData
            pairingState.emit(PairingState.QrCodeGenerated(jsonQrData))
        }

        return pairingState
    }


    fun generateQqCode(): QrConnectionConfig {
        val lanAddresses = network.addresses()
        val primaryIp = lanAddresses.firstOrNull() ?: network.addresses().firstOrNull() ?: "127.0.0.1"

        return QrConnectionConfig(
            ip = primaryIp,
            port = checkNotNull(boundPort) { "Server is not listening" },
            secretKey = cryptoEngine.generateKey(),
            pairingKey = (10000..99999).random().toString(),
            ipAddresses = lanAddresses
        )
    }

    private suspend fun emitPairingConnected(device: DeviceData) {
        pairingState.emit(PairingState.Connected(device))
    }

    private suspend fun emitPairingByActiveConnections() {
        val firstConnectedDeviceId = connectionsMutex.withLock { connections.keys.firstOrNull() }
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
        const val DEFAULT_PORT = 33333
        const val SERVER_PORT_KEY = "sync.serverPort"
        const val SERVER_EVENT_BUFFER_SIZE = 64
    }
}

private fun CopiedData.isSyncableText(): Boolean {
    return this is CopiedData.Text || this is CopiedData.FormattedText
}

private fun Application.jsonInit(json: Json) {
    install(ContentNegotiation) { json(json) }
}

private fun Application.converterInit() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}
