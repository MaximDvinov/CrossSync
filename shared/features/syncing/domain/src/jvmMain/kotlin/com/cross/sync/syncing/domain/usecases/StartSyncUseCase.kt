package com.cross.sync.syncing.domain.usecases

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.notifications.domain.repository.NotificationRepository
import com.cross.sync.syncing.domain.entity.ServerEvent
import com.cross.sync.syncing.domain.entity.ServerState
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.repository.DeviceRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StartSyncUseCase(
    private val server: ClipboardServer,
    private val systemClipboardRepository: SystemClipboardRepository,
    private val localClipboardRepository: LocalClipboardRepository,
    private val deviceRepository: DeviceRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): StateFlow<ServerState> = coroutineScope {
        val (stateFlow, eventFlow) = server.start()
        var lastOutboundKey: String? = null
        var lastOutboundAtMs: Long = 0L
        var suppressOutboundKey: String? = null
        var suppressOutboundUntilMs: Long = 0L

        launch {
            stateFlow.collect { state ->
                when (state) {
                    is ServerState.Error -> {}
                    is ServerState.Started -> {}
                    is ServerState.Starting -> {}
                    is ServerState.Stopped -> {}
                }
            }
        }

        launch {
            systemClipboardRepository.observeData().collect {
                val message = it ?: return@collect
                if (!message.isSyncableText()) return@collect
                val now = System.currentTimeMillis()
                val key = message.normalizedClipboardKey()

                if (key == suppressOutboundKey && now <= suppressOutboundUntilMs) return@collect
                if (key == lastOutboundKey && now - lastOutboundAtMs <= OUTBOUND_DEDUP_WINDOW_MS) return@collect

                server.sendCopiedDataWebSocket(message)
                    .onSuccess {
                        lastOutboundKey = key
                        lastOutboundAtMs = now
                    }
            }
        }


        launch {
            eventFlow.collect { event ->
                when (event) {
                    is ServerEvent.AddedDevice -> {
                        deviceRepository.saveDevice(event.deviceData)
                    }

                    is ServerEvent.ReceivedCopiedData -> {
                        suppressOutboundKey = event.copiedData.normalizedClipboardKey()
                        suppressOutboundUntilMs = System.currentTimeMillis() + SUPPRESS_OUTBOUND_WINDOW_MS
                        systemClipboardRepository.setData(event.copiedData)
                    }

                    is ServerEvent.ReceivedNotification -> {
                        notificationRepository.upsert(event.notification)
                    }

                    is ServerEvent.RemovedNotification -> {
                        notificationRepository.markRemoved(
                            deviceId = event.deviceId,
                            notificationKey = event.removal.notificationKey,
                            removedAt = event.removal.removedAt,
                        )
                    }
                }
            }
        }

        return@coroutineScope stateFlow
    }

    companion object {
        private const val SUPPRESS_OUTBOUND_WINDOW_MS = 3_000L
        private const val OUTBOUND_DEDUP_WINDOW_MS = 2_000L
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
