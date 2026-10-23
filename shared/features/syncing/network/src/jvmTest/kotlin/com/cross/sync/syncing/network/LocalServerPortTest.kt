package com.cross.sync.syncing.network

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerState
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.network.crypto.AesGcmCryptoEngine
import com.cross.sync.syncing.network.model.QrConnectionConfig
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalServerPortTest {
    @Test
    fun occupiedPortFallsBackAndPairingUsesTheListeningPort() = runTest {
        ServerSocket(0).use { occupied ->
            val settings = PropertiesSettings(Properties())
            val server = createServer(occupied.localPort, settings)
            try {
                val pairing = server.startPairing()
                val result = withContext(Dispatchers.Default) {
                    withTimeout(10000) { pairing.first { it is PairingState.QrCodeGenerated || it is PairingState.Error } }
                }
                val qr = Json.decodeFromString<QrConnectionConfig>(assertIs<PairingState.QrCodeGenerated>(result).qrCode)
                assertTrue(qr.port in 1..65535)
                assertNotEquals(occupied.localPort, qr.port)
                assertEquals(qr.port, settings.getIntOrNull("sync.serverPort"))
                val status = withContext(Dispatchers.IO) {
                    val connection = URI("http://127.0.0.1:${qr.port}/pair").toURL().openConnection() as HttpURLConnection
                    try {
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.outputStream.use {
                            it.write("""{"id":"phone","name":"Test phone","pairingKey":"${qr.pairingKey}"}""".toByteArray())
                        }
                        connection.responseCode
                    } finally {
                        connection.disconnect()
                    }
                }
                assertEquals(200, status)
                assertIs<ServerState.Started>(server.observeServerState().value)
            } finally {
                server.stopAndJoin()
            }
        }
    }

    @Test
    fun selectedPortIsReusedAfterRestart() = runTest {
        val settings = PropertiesSettings(Properties())
        val first = createServer(0, settings)
        val selectedPort: Int
        try {
            first.start()
            assertIs<ServerState.Started>(awaitStartup(first))
            selectedPort = first.generateQqCode().port
        } finally {
            first.stopAndJoin()
        }
        val restarted = createServer(settings = settings)
        try {
            restarted.start()
            assertIs<ServerState.Started>(awaitStartup(restarted))
            assertEquals(selectedPort, restarted.generateQqCode().port)
        } finally {
            restarted.stopAndJoin()
        }
    }

    @Test
    fun invalidPortRemainsAnErrorAndDoesNotProduceQrCode() = runTest {
        val settings = PropertiesSettings(Properties())
        val server = createServer(65536, settings)
        try {
            server.start()
            assertIs<ServerState.Error>(awaitStartup(server))
            val pairing = server.startPairing()
            withContext(Dispatchers.Default) {
                withTimeout(10000) { pairing.first { it is PairingState.Error } }
            }
            assertNull(settings.getIntOrNull("sync.serverPort"))
        } finally {
            server.stopAndJoin()
        }
    }

    private suspend fun awaitStartup(server: LocalServer): ServerState = withContext(Dispatchers.Default) {
        withTimeout(10000) { server.observeServerState().first { it !is ServerState.Starting } }
    }

    private fun createServer(port: Int = 33333, settings: Settings): LocalServer = LocalServer(
        cryptoEngine = AesGcmCryptoEngine(),
        deviceRepository = object : DeviceRepository {
            private val devices = MutableStateFlow<List<DeviceData>>(emptyList())
            override suspend fun saveDevice(deviceData: DeviceData) { devices.value = devices.value + deviceData }
            override suspend fun getDeviceById(id: String?) = devices.value.firstOrNull { it.id == id }
            override suspend fun deleteDevice(id: String) { devices.value = devices.value.filterNot { it.id == id } }
            override fun observeDevices() = devices
        },
        localClipboardRepository = UnusedClipboardRepository,
        systemClipboardRepository = object : SystemClipboardRepository {
            private val data = MutableStateFlow<CopiedData?>(null)
            override suspend fun setData(copiedData: CopiedData) { data.value = copiedData }
            override suspend fun getData() = data.value
            override fun observeData() = data
            override fun initClipboardManager() = data
        },
        tokens = object : ServerTokens {
            override fun access(deviceId: String) = "access"
            override fun refresh(deviceId: String) = "refresh"
            override fun verifyAccess(token: String): String? = null
            override fun verifyRefresh(token: String): String? = null
        },
        network = object : ServerNetworkInfo {
            override fun addresses() = listOf("127.0.0.1")
            override fun name() = "Test server"
        },
        port = port,
        settings = settings,
    )

    private object UnusedClipboardRepository : LocalClipboardRepository {
        override suspend fun addCopiedData(copiedData: CopiedData): Unit = error("Unused")
        override suspend fun getLastCopiedData(): CopiedData? = error("Unused")
        override suspend fun getCopiedDataById(id: Long): CopiedData? = error("Unused")
        override suspend fun deleteCopiedDataById(id: Long): Unit = error("Unused")
        override suspend fun addCategory(category: Category): Unit = error("Unused")
        override suspend fun updateCategory(category: Category): Unit = error("Unused")
        override suspend fun getCategoryById(id: Long): Category? = error("Unused")
        override suspend fun deleteCategoryById(id: Long): Unit = error("Unused")
        override suspend fun addCopiedDataToCategory(copiedDataId: Long, categoryId: Long): Unit = error("Unused")
        override suspend fun moveCopiedDataToCategory(copiedDataId: Long, fromCategoryId: Long?, toCategoryId: Long): Unit = error("Unused")
        override suspend fun getAllCopiedData(): List<CopiedData> = error("Unused")
        override suspend fun clearAllCopiedDataInCategory(categoryId: Long?): Unit = error("Unused")
        override suspend fun clearUncategorizedCopiedData(): Unit = error("Unused")
        override suspend fun clearCopiedDataOlderThan(olderThanEpochMillis: Long): Unit = error("Unused")
        override fun observeCopiedData() = error("Unused")
        override fun observeCopiedDataByCategory(categoryId: Long) = error("Unused")
        override fun observeUncategorizedCopiedData() = error("Unused")
        override fun observeCategories() = error("Unused")
    }
}
