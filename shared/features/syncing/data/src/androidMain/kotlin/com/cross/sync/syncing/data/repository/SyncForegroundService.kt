package com.cross.sync.syncing.data.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.network.ClipboardClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SyncForegroundService : LifecycleService() {
    private val client: ClipboardClient by inject()
    private val clipboardRepository: LocalClipboardRepository by inject()
    private val systemClipboardRepository: SystemClipboardRepository by inject()

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var isStarted = false
    private var pendingClipboardData: CopiedData? = null
    private var ignoreOutboundClipboardId: Long? = null
    private var ignoreOutboundClipboardKey: String? = null
    private var ignoreOutboundUntilMs: Long = 0L
    private var lastSentClipboardId: Long? = null
    private var lastIncomingClipboardId: Long? = null
    private var lastIncomingClipboardKey: String? = null
    private var lastIncomingAtMs: Long = 0L
    private var lastObservedClipboardKey: String? = null
    private var lastObservedClipboardAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()

        Log.i("SyncService", "onStartCommand: websocket start")


    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)
        val notificationManager = SyncNotificationManager(this)
        notificationManager.createChannel()

        startForeground(
            SyncNotificationManager.NOTIFICATION_ID,
            notificationManager.buildNotification()
        )

        Log.i("SyncService", "onStartCommand: websocket start")
        if (!isStarted) {
            isStarted = true
            startSyncJobs()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startSyncJobs() {
        serviceScope.launch {
            // Keep websocket alive and reconnect automatically on any disconnection.
            launch { reconnectLoop() }

            // Receive updates from Mac and apply on Android.
            launch {
                client.observeCopiedData().collect { copiedData ->
                    val now = System.currentTimeMillis()
                    val incomingKey = copiedData.normalizedClipboardKey()
                    val isDuplicateIncoming =
                        copiedData.id == lastIncomingClipboardId ||
                            (incomingKey == lastIncomingClipboardKey &&
                                now - lastIncomingAtMs <= INCOMING_DEDUP_WINDOW_MS)
                    if (isDuplicateIncoming) {
                        Log.i("SyncService", "skip duplicate incoming: $copiedData")
                        return@collect
                    }

                    lastIncomingClipboardId = copiedData.id
                    lastIncomingClipboardKey = incomingKey
                    lastIncomingAtMs = now

                    Log.i("SyncService", "incoming: $copiedData")

                    ignoreOutboundClipboardId = copiedData.id
                    ignoreOutboundClipboardKey = incomingKey
                    ignoreOutboundUntilMs = System.currentTimeMillis() + IGNORE_OUTBOUND_WINDOW_MS
                    pendingClipboardData = copiedData

                    systemClipboardRepository.setData(copiedData)
                    persistIfNew(copiedData)
                }
            }

            // Track phone clipboard and push latest value to Mac.
            launch {
                systemClipboardRepository.initClipboardManager().collect { copiedData ->
                    copiedData ?: return@collect

                    val now = System.currentTimeMillis()
                    val observedKey = copiedData.normalizedClipboardKey()

                    if (observedKey == lastObservedClipboardKey &&
                        now - lastObservedClipboardAtMs <= OUTBOUND_DEDUP_WINDOW_MS
                    ) {
                        return@collect
                    }
                    lastObservedClipboardKey = observedKey
                    lastObservedClipboardAtMs = now

                    val isEchoById = copiedData.id == ignoreOutboundClipboardId
                    val isEchoByContent =
                        observedKey == ignoreOutboundClipboardKey &&
                            now <= ignoreOutboundUntilMs

                    if (isEchoById || isEchoByContent) {
                        pendingClipboardData = null
                        return@collect
                    }

                    if (now > ignoreOutboundUntilMs) {
                        ignoreOutboundClipboardId = null
                        ignoreOutboundClipboardKey = null
                    }

                    // Persist phone clipboard changes immediately, even when Clipboard screen is not visible.
                    persistIfNew(copiedData)
                    pendingClipboardData = copiedData
                    trySendPendingClipboardData()
                }
            }

            // Flush latest pending clipboard right after connection is restored.
            launch {
                client.observeConnectedState().collect { state ->
                    if (state is ClientConnectState.Connected) {
                        trySendPendingClipboardData()
                    }
                }
            }
        }
    }

    private suspend fun reconnectLoop() {
        while (serviceScope.isActive) {
            when (client.observeConnectedState().value) {
                is ClientConnectState.Connected,
                is ClientConnectState.Connecting -> {
                    delay(RECONNECT_DELAY_MS)
                }

                is ClientConnectState.Idle,
                is ClientConnectState.Disconnected,
                is ClientConnectState.Error -> {
                    runCatching {
                        client.connect()
                    }.onFailure {
                        Log.e("SyncService", "connect failed: ${it.message}", it)
                    }
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private suspend fun trySendPendingClipboardData() {
        val clipboardData = pendingClipboardData ?: return
        if (clipboardData.id == lastSentClipboardId) {
            pendingClipboardData = null
            return
        }

        if (client.observeConnectedState().value !is ClientConnectState.Connected) return

        client.sendCopiedData(clipboardData)
            .onSuccess {
                lastSentClipboardId = clipboardData.id
                pendingClipboardData = null
                Log.i("SyncService", "outgoing sent: $clipboardData")
            }
            .onFailure {
                Log.e("SyncService", "outgoing send failed: ${it.message}", it)
            }
    }

    private suspend fun persistIfNew(copiedData: CopiedData) {
        val lastStoredData = clipboardRepository.getLastCopiedData()
        if (lastStoredData?.normalizedClipboardKey() == copiedData.normalizedClipboardKey()) {
            return
        }
        clipboardRepository.addCopiedData(copiedData)
    }

    companion object {
        private const val RECONNECT_DELAY_MS = 1_500L
        private const val IGNORE_OUTBOUND_WINDOW_MS = 2_500L
        private const val INCOMING_DEDUP_WINDOW_MS = 4_000L
        private const val OUTBOUND_DEDUP_WINDOW_MS = 2_500L
    }
}

private fun CopiedData.normalizedClipboardKey(): String {
    return when (this) {
        // Text and formatted text should map to the same logical key to avoid echo duplicates.
        is CopiedData.Text -> "plain:${text.normalizeClipboardText()}"
        is CopiedData.FormattedText -> "plain:${plainText.normalizeClipboardText()}"
        is CopiedData.Image -> "image:$imagePath"
        is CopiedData.File -> "file:${filePaths.joinToString("|")}"
    }
}

private fun String.normalizeClipboardText(): String {
    return trim()
        .replace("\r\n", "\n")
        .replace(Regex("\\s+"), " ")
}

class SyncNotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "sync_channel"
        const val NOTIFICATION_ID = 1001
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Clipboard synchronization service"
                setShowBadge(false)
            }

            context
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun buildNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("CrossSync")
            .setContentText("Синхронизация буфера обмена активна")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
