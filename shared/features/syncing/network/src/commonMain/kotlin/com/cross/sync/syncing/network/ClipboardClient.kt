package com.cross.sync.syncing.network

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.notifications.domain.entity.NotificationActionRequest
import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.SyncSettingsKeys
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.network.crypto.CryptoEngine
import com.cross.sync.syncing.network.model.CopiedDataDto
import com.cross.sync.syncing.network.model.DataPackage
import com.cross.sync.syncing.network.model.DeviceDataDto
import com.cross.sync.syncing.network.model.QrConnectionConfig
import com.cross.sync.syncing.network.model.RefreshRequest
import com.cross.sync.syncing.network.model.SyncPayloadDto
import com.cross.sync.syncing.network.model.TokenResponse
import com.cross.sync.syncing.network.model.toDomain
import com.cross.sync.syncing.network.model.toDto
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class ClipboardClient(
    private val cryptoEngine: CryptoEngine,
    private val deviceRepository: DeviceRepository,
    private val localClipboardRepository: LocalClipboardRepository,
    private val setting: Settings
) {
    private val json: Json = Json
    private var client: HttpClient? = null
    private var config: QrConnectionConfig? = null
    private val messagesFlow = MutableSharedFlow<IncomingCopiedData>(1)
    private val categoryFlow = MutableSharedFlow<Category>(1)
    private val categoryBindingFlow = MutableSharedFlow<CategoryBinding>(1)
    // Actions are commands with one consumer (the Android sync service). A channel
    // keeps commands received during reconnect/startup instead of dropping them as
    // a replay=0 SharedFlow would.
    private val notificationActionChannel = Channel<NotificationActionRequest>(Channel.BUFFERED)

    private val connectedState = MutableStateFlow<ClientConnectState>(ClientConnectState.Idle)

    suspend fun pairToServer(
        deviceId: String,
        deviceName: String,
        qrConnectionConfig: String
    ): Result<Unit> = runCatchingForApi {
        config = json.decodeFromString<QrConnectionConfig>(qrConnectionConfig)

        setting[SyncSettingsKeys.PORT] = config!!.port

        var lastError: Throwable? = null
        val candidates = config!!.candidateHosts()

        for (host in candidates) {
            setting[SyncSettingsKeys.HOST] = host
            client?.close()

            val httpClient = createHttpClient(json, deviceRepository, setting, host).also {
                client = it
            }

            val tokens = runCatching {
                httpClient.post(PAIR_ROUTE) {
                    setBody(
                        DeviceDataDto(
                            id = deviceId,
                            name = deviceName,
                            pairingKey = config!!.pairingKey
                        )
                    )
                }.body<TokenResponse>()
            }.onFailure {
                lastError = it
                httpClient.close()
                if (client === httpClient) client = null
            }.getOrNull() ?: continue

            val knownHosts = listOf(tokens.serverIp, config!!.ip)
                .filterNotNull()
                .plus(tokens.serverIps)
                .plus(config!!.ipAddresses)
                .normalizedHosts()

            setting[SyncSettingsKeys.HOST] = host
            setting[SyncSettingsKeys.HOSTS] = knownHosts.joinToString(",")
            setting[SyncSettingsKeys.CONNECTED_DESKTOP_NAME] = tokens.serverName ?: "Mac"
            setting[SyncSettingsKeys.CONNECTED_DESKTOP_IP] = host

            deviceRepository.saveDevice(
                DeviceData(
                    id = deviceId,
                    name = deviceName,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    secretKey = config!!.secretKey
                )
            )

            return@runCatchingForApi
        }

        throw lastError ?: ClientException()
    }

    fun disconnect() {
        runCatching {
            client?.close()
        }
        client = null
        connectedState.value = ClientConnectState.Idle
    }

    suspend fun connect(): Flow<ClientConnectState> {
        Napier.log(
            io.github.aakira.napier.LogLevel.INFO,
            "Websocket",
            message = "start connect"
        )
        connectedState.emit(ClientConnectState.Connecting)

        val candidates = savedHosts()
        var lastError: Throwable? = null

        for (host in candidates) {
            try {
                client?.close()
                val httpClient = createHttpClient(json, deviceRepository, setting, host).apply {
                    client = this
                }

                httpClient.webSocket(
                    method = HttpMethod.Get,
                    path = SYNC_ROUTE
                ) {
                    setting[SyncSettingsKeys.HOST] = host
                    setting[SyncSettingsKeys.CONNECTED_DESKTOP_IP] = host

                    val device = deviceRepository.getDeviceById(null)
                        ?: error("Device not found")

                    val knownCopiedDataIds = localClipboardRepository.getAllCopiedData()
                        .mapTo(mutableSetOf()) { it.id }
                    val snapshotRequest = SyncPayloadDto.SnapshotRequestPayload(knownCopiedDataIds)
                    val requestJson = json.encodeToString<SyncPayloadDto>(snapshotRequest)
                    sendSerialized(
                        DataPackage(
                            cryptoEngine.encrypt(requestJson, secretKey = device.secretKey)
                        )
                    )

                    connectedState.emit(ClientConnectState.Connected)

                    Napier.log(
                        io.github.aakira.napier.LogLevel.INFO,
                        "Websocket",
                        message = "connected"
                    )

                    try {
                        while (true) {
                            val data = receiveDeserialized<DataPackage>()

                            cryptoEngine.decrypt(
                                data.encryptedContent,
                                secretKey = device.secretKey
                            ).onSuccess {
                                val payload = runCatching {
                                    json.decodeFromString<SyncPayloadDto>(it)
                                }.getOrNull()

                                when (payload) {
                                    is SyncPayloadDto.CopiedDataPayload -> {
                                        messagesFlow.emit(
                                            IncomingCopiedData(
                                                data = payload.data.toDomain(),
                                                updateSystemClipboard = true
                                            )
                                        )
                                    }

                                    is SyncPayloadDto.HistoryCopiedDataPayload -> {
                                        messagesFlow.emit(
                                            IncomingCopiedData(
                                                data = payload.data.toDomain(),
                                                updateSystemClipboard = false
                                            )
                                        )
                                    }

                                    is SyncPayloadDto.CurrentClipboardPayload -> {
                                        messagesFlow.emit(
                                            IncomingCopiedData(
                                                data = payload.data.toDomain(),
                                                updateSystemClipboard = true
                                            )
                                        )
                                    }

                                    is SyncPayloadDto.CategoryPayload -> {
                                        categoryFlow.emit(
                                            Category(
                                                id = payload.categoryId,
                                                name = payload.name
                                            )
                                        )
                                    }

                                    is SyncPayloadDto.CategoryBindingPayload -> {
                                        categoryBindingFlow.emit(
                                            CategoryBinding(
                                                categoryId = payload.categoryId,
                                                copiedDataId = payload.copiedDataId
                                            )
                                        )
                                    }

                                    is SyncPayloadDto.NotificationActionPayload -> {
                                        notificationActionChannel.send(payload.request)
                                    }

                                    null -> {
                                        // Backward compatibility: previously only CopiedDataDto was sent.
                                        val message = json.decodeFromString<CopiedDataDto>(it)
                                        messagesFlow.emit(
                                            IncomingCopiedData(
                                                data = message.toDomain(),
                                                updateSystemClipboard = true
                                            )
                                        )
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        e.printStackTrace()
                        connectedState.emit(
                            ClientConnectState.Disconnected(e)
                        )
                        throw e
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        connectedState.emit(
                            ClientConnectState.Disconnected(e)
                        )
                    }
                }

                httpClient.close()
                if (client === httpClient) client = null
                return connectedState
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = e
                client?.close()
                client = null
            }
        }

        val error = lastError ?: ClientException()
        error.printStackTrace()
        connectedState.emit(
            ClientConnectState.Disconnected(error)
        )

        return connectedState
    }

    suspend fun sendCopiedData(copiedData: CopiedData): Result<Unit> = runCatchingForApi {
        sendPayloadToServer(SyncPayloadDto.CopiedDataPayload(copiedData.toDto()))
    }

    suspend fun sendNotification(notification: SyncedNotification): Result<Unit> = runCatchingForApi {
        sendPayloadToServer(SyncPayloadDto.NotificationPayload(notification))
    }

    suspend fun removeNotification(removal: NotificationRemoval): Result<Unit> = runCatchingForApi {
        sendPayloadToServer(SyncPayloadDto.NotificationRemovedPayload(removal))
    }

    suspend fun observeCopiedData(): Flow<IncomingCopiedData> {
        return messagesFlow
    }

    suspend fun observeCategories(): Flow<Category> {
        return categoryFlow
    }

    suspend fun observeCategoryBindings(): Flow<CategoryBinding> {
        return categoryBindingFlow
    }

    fun observeNotificationActions(): Flow<NotificationActionRequest> = notificationActionChannel.receiveAsFlow()

    fun observeConnectedState(): StateFlow<ClientConnectState> {
        return connectedState
    }

    private fun savedHosts(): List<String> {
        return listOf(setting[SyncSettingsKeys.HOST, ""])
            .plus(setting[SyncSettingsKeys.HOSTS, ""].split(','))
            .normalizedHosts()
    }

    private suspend fun sendPayloadToServer(payload: SyncPayloadDto) {
        val httpClient = client ?: throw ClientException()
        val device = deviceRepository.getDeviceById(null) ?: throw ClientException()
        val plain = json.encodeToString<SyncPayloadDto>(payload)
        httpClient.post(SEND_ROUTE) {
            setBody(DataPackage(cryptoEngine.encrypt(plain, secretKey = device.secretKey)))
        }
    }

    companion object {}
}

data class CategoryBinding(
    val categoryId: Long,
    val copiedDataId: Long
)

data class IncomingCopiedData(
    val data: CopiedData,
    val updateSystemClipboard: Boolean
)

private fun createHttpClient(
    json: Json,
    deviceRepository: DeviceRepository,
    setting: Settings,
    host: String = setting[SyncSettingsKeys.HOST, ""],
): HttpClient {
    return HttpClient {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 5_000
        }
        install(WebSockets) {
            pingIntervalMillis = 60_000
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(ContentNegotiation) { json(json) }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.log(
                        io.github.aakira.napier.LogLevel.VERBOSE,
                        tag = "HttpRequest",
                        message = message
                    )
                }
            }

            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val device = deviceRepository.getDeviceById(null)
                    device?.let {
                        BearerTokens(device.accessToken, device.refreshToken)
                    }
                }

                refreshTokens {
                    val device = deviceRepository.getDeviceById(null)
                        ?: throw ClientException("Device not found")
                    val tokens = client.post(AUTH_REFRESH_ROUTE) {
                        setBody(RefreshRequest(device.refreshToken))
                        markAsRefreshTokenRequest()
                    }.body<TokenResponse>()

                    deviceRepository.saveDevice(
                        device.copy(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken
                        )
                    )

                    BearerTokens(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken
                    )
                }
            }
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                this.host = host
                this.port = setting[SyncSettingsKeys.PORT, 0]
            }
            headers.appendIfNameAbsent(HttpHeaders.ContentType, "application/json")
        }
    }
}

private fun QrConnectionConfig.candidateHosts(): List<String> {
    return listOf(ip)
        .plus(ipAddresses)
        .normalizedHosts()
}

private fun Iterable<String>.normalizedHosts(): List<String> {
    return map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}
