package com.cross.sync.syncing.runtime

import com.cross.sync.clipboard.domain.usecase.ClearCopiedDataOlderThanUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.notifications.domain.usecase.ClearNotificationHistoryUseCase
import com.cross.sync.notifications.domain.usecase.ObserveNotificationsUseCase
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.usecases.StartSyncUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** One per application; independent of screen and ViewModel subscriptions. */
class DesktopRuntime(
    private val server: ClipboardServer,
    private val initializeClipboard: InitClipboardManagerUseCase,
    private val sync: StartSyncUseCase,
    private val clearClipboard: ClearCopiedDataOlderThanUseCase,
    private val clearNotifications: ClearNotificationHistoryUseCase,
    private val preferences: SettingPreferencesStore,
    private val notifications: ObserveNotificationsUseCase,
    private val delivery: NotificationDeliveryCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false
    private var syncJob: Job? = null
    fun start() {
        if (started) return
        started = true
        scope.launch { initializeClipboard() }
        scope.launch {
            // Read persisted history before the server can receive fresh notifications.
            delivery.onChanged(notifications.active().first())
            launch { notifications.active().collect { delivery.onChanged(it) } }
            syncJob = launch { sync() }
        }
        scope.launch {
            while (true) {
                val settings = preferences.getGeneralSettings()
                val now = Clock.System.now().toEpochMilliseconds()
                attempt { clearClipboard(now - settings.clipboardAutoClearTimeoutDays.coerceAtLeast(1) * DAY) }
                attempt { clearNotifications.before(now - settings.notificationHistoryRetentionDays.coerceAtLeast(1) * DAY) }
                delay(60 * 60_000L)
            }
        }
    }
    fun close() {
        if (!started) return
        syncJob?.cancel()
        server.stop()
        scope.cancel()
        started = false
    }
    suspend fun shutdown() {
        scope.cancel()
        scope.coroutineContext[Job]?.join()
        server.stopAndJoin()
        started = false
    }
    private suspend fun attempt(action: suspend () -> Unit) {
        try { action() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
    }
    private companion object { const val DAY = 86_400_000L }
}
