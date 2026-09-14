package com.cross.sync.syncing.data.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
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
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.network.ClipboardClient
import com.cross.sync.notifications.domain.repository.NotificationActionExecutor
import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SyncForegroundService : LifecycleService() {
    private val client: ClipboardClient by inject()
    private val clipboardRepository: LocalClipboardRepository by inject()
    private val systemClipboardRepository: SystemClipboardRepository by inject()
    private val deviceRepository: DeviceRepository by inject()
    private val notificationActionExecutor: NotificationActionExecutor by inject()
    private val notificationSnapshotPublisher: NotificationSnapshotPublisher by inject()

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var isStarted = false
    private var lastIncomingClipboardId: Long? = null
    private var lastIncomingSyncKey: String? = null
    private var lastIncomingSyncAtMs: Long = 0L
    private var lastIncomingSyncData: CopiedData? = null

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
        if (intent?.action == ACTION_STOP_SYNC) {
            stopServiceNow()
            return START_NOT_STICKY
        }

        val notificationManager = SyncNotificationManager(this)
        notificationManager.createChannel()

        startAsConnectedDeviceForeground(notificationManager.buildNotification())

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
            val copiedDataCollectorReady = CompletableDeferred<Unit>()
            val categoryCollectorReady = CompletableDeferred<Unit>()
            val categoryBindingCollectorReady = CompletableDeferred<Unit>()
            val notificationActionCollectorReady = CompletableDeferred<Unit>()

            // Persist history from Mac, but only apply the current/live value to Android's clipboard.
            launch {
                client.observeCopiedData()
                    .onStart { copiedDataCollectorReady.complete(Unit) }
                    .collect { incomingData ->
                        val copiedData = incomingData.data
                        if (!copiedData.isSyncableText()) return@collect
                        if (!incomingData.updateSystemClipboard) {
                            persistIfMissing(copiedData)
                            return@collect
                        }
                        if (copiedData.id == lastIncomingClipboardId) {
                            return@collect
                        }
                        if (shouldSkipIncomingDuplicate(copiedData)) {
                            return@collect
                        }
                        lastIncomingClipboardId = copiedData.id
                        rememberIncoming(copiedData)

                        Log.i("SyncService", "incoming: $copiedData")

                        systemClipboardRepository.setData(copiedData)
                        persistIfMissing(copiedData)
                    }
            }

            // Receive categories from Mac and mirror them on Android.
            launch {
                client.observeCategories()
                    .onStart { categoryCollectorReady.complete(Unit) }
                    .collect { category ->
                        clipboardRepository.addCategory(category)
                    }
            }

            // Receive category bindings from Mac and mirror relations.
            launch {
                client.observeCategoryBindings()
                    .onStart { categoryBindingCollectorReady.complete(Unit) }
                    .collect { binding ->
                        runCatching {
                            clipboardRepository.addCopiedDataToCategory(
                                copiedDataId = binding.copiedDataId,
                                categoryId = binding.categoryId
                            )
                        }.onFailure {
                            Log.w(
                                "SyncService",
                                "failed to apply category binding ${binding.categoryId} -> ${binding.copiedDataId}: ${it.message}"
                            )
                        }
                    }
            }

            launch {
                client.observeNotificationActions()
                    .onStart { notificationActionCollectorReady.complete(Unit) }
                    .collect { request ->
                    notificationActionExecutor.execute(request)
                        .onFailure { error ->
                            Log.w("SyncService", "notification action failed: ${error.message}", error)
                        }
                    }
            }

            launch {
                client.observeConnectedState().collect { state ->
                    if (state is ClientConnectState.Connected) {
                        notificationSnapshotPublisher.publishActive()
                    }
                }
            }

            copiedDataCollectorReady.await()
            categoryCollectorReady.await()
            categoryBindingCollectorReady.await()
            notificationActionCollectorReady.await()

            // Start networking only after every snapshot consumer is ready.
            reconnectLoop()
        }
    }

    private suspend fun reconnectLoop() {
        var reconnectAttempt = 0
        while (serviceScope.isActive) {
            if (deviceRepository.getDeviceById(null) == null) {
                stopServiceNow()
                break
            }

            if (!hasLocalNetworkAccess()) {
                Log.i("SyncService", "local network permission is unavailable; stopping sync")
                stopServiceNow()
                break
            }

            when (client.observeConnectedState().value) {
                is ClientConnectState.Connected,
                is ClientConnectState.Connecting -> {
                    reconnectAttempt = 0
                    delay(CONNECTION_STATE_CHECK_DELAY_MS)
                }

                is ClientConnectState.Idle,
                is ClientConnectState.Disconnected,
                is ClientConnectState.Error -> {
                    if (reconnectAttempt > 0) {
                        delay(reconnectDelayMillis(reconnectAttempt))
                    }
                    runCatching {
                        client.connect()
                    }.onFailure {
                        Log.e("SyncService", "connect failed: ${it.message}", it)
                    }
                    reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(MAX_RECONNECT_ATTEMPTS)
                }
            }
        }
    }

    private suspend fun persistIfMissing(copiedData: CopiedData) {
        if (clipboardRepository.getCopiedDataById(copiedData.id) != null) return
        clipboardRepository.addCopiedData(copiedData)
    }

    private suspend fun shouldSkipIncomingDuplicate(copiedData: CopiedData): Boolean {
        val key = copiedData.normalizedClipboardKey()
        val now = System.currentTimeMillis()
        val isWithinWindow = key == lastIncomingSyncKey && now - lastIncomingSyncAtMs <= INCOMING_DEDUP_WINDOW_MS
        if (!isWithinWindow) return false

        val previousData = lastIncomingSyncData
        val isUpgradeToFormatted = previousData is CopiedData.Text && copiedData is CopiedData.FormattedText
        if (!isUpgradeToFormatted) return true

        // Replace the previously persisted plain text with formatted text for the same payload.
        clipboardRepository.deleteCopiedDataById(previousData.id)
        return false
    }

    private fun rememberIncoming(copiedData: CopiedData) {
        lastIncomingSyncKey = copiedData.normalizedClipboardKey()
        lastIncomingSyncAtMs = System.currentTimeMillis()
        lastIncomingSyncData = copiedData
    }

    private fun stopServiceNow() {
        client.disconnect()
        stopForeground(true)
        stopSelf()
    }

    private fun startAsConnectedDeviceForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SyncNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(SyncNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_STOP_SYNC = "com.cross.sync.ACTION_STOP_SYNC"
        private const val CONNECTION_STATE_CHECK_DELAY_MS = 30_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 5_000L
        private const val MAX_RECONNECT_DELAY_MS = 5 * 60_000L
        private const val MAX_RECONNECT_ATTEMPTS = 6
        private const val INCOMING_DEDUP_WINDOW_MS = 2_500L

        private fun reconnectDelayMillis(attempt: Int): Long {
            return (INITIAL_RECONNECT_DELAY_MS * (1L shl (attempt - 1)))
                .coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
    }
}

private fun CopiedData.isSyncableText(): Boolean {
    return this is CopiedData.Text || this is CopiedData.FormattedText
}

private fun CopiedData.normalizedClipboardKey(): String {
    return when (this) {
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
        private const val REQUEST_CODE_STOP_SYNC = 1002
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
                setSound(null, null)
                enableVibration(false)
            }

            context
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun buildNotification(): Notification {
        val stopIntent = Intent(context, SyncForegroundService::class.java).apply {
            action = SyncForegroundService.ACTION_STOP_SYNC
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_STOP_SYNC,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("CrossSync")
            .setContentText("Clipboard synchronization is active")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
}
