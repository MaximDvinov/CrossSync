package com.cross.sync.syncing.network

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.network.crypto.CryptoEngine
import com.cross.sync.syncing.network.model.CopiedDataDto
import com.cross.sync.syncing.network.model.DataPackage
import com.cross.sync.syncing.network.model.DeviceDataDto
import com.cross.sync.syncing.network.model.QrConnectionConfig
import com.cross.sync.syncing.network.model.RefreshRequest
import com.cross.sync.syncing.network.model.TokenResponse
import com.cross.sync.syncing.network.model.toDomain
import com.cross.sync.syncing.network.model.toDto
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class ClipboardClient(
    private val cryptoEngine: CryptoEngine,
    private val deviceRepository: DeviceRepository,
    private val setting: Settings
) {
    private val json: Json = Json
    private var client: HttpClient? = null
    private var config: QrConnectionConfig? = null
    private val messagesFlow = MutableSharedFlow<CopiedData>(1)

    private val connectedState = MutableStateFlow<ClientConnectState>(ClientConnectState.Idle)

    suspend fun pairToServer(
        deviceId: String,
        deviceName: String,
        qrConnectionConfig: String
    ): Result<Unit> = runCatchingForApi {
        config = json.decodeFromString<QrConnectionConfig>(qrConnectionConfig)

        setting[HOST] = config!!.ip
        setting[PORT] = config!!.port

        client = createHttpClient(json, deviceRepository, setting)

        val tokens = client!!.post(PAIR_ROUTE) {
            setBody(
                DeviceDataDto(
                    id = deviceId,
                    name = deviceName,
                    pairingKey = config!!.pairingKey
                )
            )
        }.body<TokenResponse>()

        deviceRepository.saveDevice(
            DeviceData(
                id = deviceId,
                name = deviceName,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                secretKey = config!!.secretKey
            )
        )
    }

    suspend fun connect(): Flow<ClientConnectState> {
        Napier.log(
            io.github.aakira.napier.LogLevel.INFO,
            "Websocket",
            message = "start connect"
        )
        val httpClient = client ?: createHttpClient(json, deviceRepository, setting).apply {
            client = this
        }

        connectedState.emit(ClientConnectState.Connecting)

        try {
            httpClient.webSocket(
                method = HttpMethod.Get,
                path = SYNC_ROUTE
            ) {
                val device = deviceRepository.getDeviceById(null)
                    ?: error("Device not found")

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
                            val message =
                                json.decodeFromString<CopiedDataDto>(it)
                            Napier.log(
                                io.github.aakira.napier.LogLevel.INFO,
                                "Websocket",
                                message = "encrypt data = $message"
                            )

                            messagesFlow.emit(message.toDomain())
                        }.onFailure {

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
        } catch (e: Throwable) {
            e.printStackTrace()
            connectedState.emit(
                ClientConnectState.Disconnected(e)
            )
        }

        return connectedState
    }

    suspend fun sendCopiedData(copiedData: CopiedData): Result<Unit> = runCatchingForApi {
        client ?: throw ClientException()
        val device = deviceRepository.getDeviceById(null) ?: throw Exception("Device not found")
        client!!.post(SEND_ROUTE) {
            val data = json.encodeToString(copiedData.toDto())

            setBody(
                DataPackage(cryptoEngine.encrypt(data, secretKey = device.secretKey))
            )
        }
    }

    suspend fun observeCopiedData(): Flow<CopiedData> {
        return messagesFlow
    }

    fun observeConnectedState(): StateFlow<ClientConnectState> {
        return connectedState
    }

    companion object {
        const val HOST: String = "host"
        const val PORT: String = "port"
    }
}

private fun createHttpClient(
    json: Json,
    deviceRepository: DeviceRepository,
    setting: Settings,
): HttpClient {
    return HttpClient {
        expectSuccess = true
        install(WebSockets) {
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
                    val tokens = client.post(AUTH_REFRESH_ROUTE) {
                        device?.refreshToken?.let { token ->
                            setBody(RefreshRequest(token))
                        }
                        markAsRefreshTokenRequest()
                    }.body<TokenResponse>()

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
                this.host = setting[ClipboardClient.HOST, ""]
                this.port = setting[ClipboardClient.PORT, 0]
            }
            headers.appendIfNameAbsent(HttpHeaders.ContentType, "application/json")
        }
    }
}
