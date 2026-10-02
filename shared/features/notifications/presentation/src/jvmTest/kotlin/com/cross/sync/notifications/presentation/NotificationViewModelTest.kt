package com.cross.sync.notifications.presentation

import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.notifications.domain.entity.NotificationAction
import com.cross.sync.notifications.domain.entity.NotificationActionRequest
import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.repository.NotificationRepository
import com.cross.sync.notifications.domain.usecase.ClearNotificationHistoryUseCase
import com.cross.sync.notifications.domain.usecase.DismissNotificationUseCase
import com.cross.sync.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cross.sync.notifications.domain.usecase.ObserveNotificationsUseCase
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerEvent
import com.cross.sync.syncing.domain.entity.ServerState
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.usecases.SendNotificationActionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    @Test
    fun offlineDismissalArchivesLocallyWithoutReportingFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeRepository()
        val server = FakeServer()
        val viewModel = viewModel(repository, server)
        try {
            viewModel.dismiss(repository.notifications.value.single())
            advanceUntilIdle()
            val stored = viewModel.state.value.notifications.single()
            assertFalse(stored.isActive)
            assertTrue(stored.isRead)
            assertNull(viewModel.state.value.actionError)
            assertTrue(viewModel.state.value.pendingActions.isEmpty())
            assertEquals(NotificationAction.Dismiss, server.requests.single().action)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedCallActionKeepsCallActiveAndRapidDoubleClickSendsOneCommand() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeRepository()
        val server = FakeServer(CompletableDeferred())
        val viewModel = viewModel(repository, server)
        try {
            val call = repository.notifications.value.single()
            viewModel.invoke(call, 1)
            viewModel.invoke(call, 1)
            advanceUntilIdle()
            assertEquals(1, server.requests.size)
            assertEquals(NotificationAction.Invoke(1), server.requests.single().action)
            assertTrue(viewModel.state.value.notifications.single().isActive)
            assertEquals(setOf("phone:call"), viewModel.state.value.pendingActions)

            server.gate?.complete(Unit)
            advanceUntilIdle()
            assertEquals("Android device is offline", viewModel.state.value.actionError)
            assertTrue(viewModel.state.value.pendingActions.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: NotificationRepository, server: ClipboardServer) = NotificationViewModel(
        ObserveNotificationsUseCase(repository),
        MarkNotificationReadUseCase(repository),
        ClearNotificationHistoryUseCase(repository),
        SendNotificationActionUseCase(server),
        DismissNotificationUseCase(repository),
    )

    private class FakeRepository : NotificationRepository {
        val notifications = MutableStateFlow(listOf(SyncedNotification(
            deviceId = "phone", notificationKey = "call", packageName = "phone.app",
            appName = "Phone", title = "Incoming call", body = "Caller",
            postedAt = 1, updatedAt = 1, kind = NotificationKind.Call,
        )))
        override fun observeNotifications() = notifications
        override suspend fun dismiss(deviceId: String, notificationKey: String) {
            notifications.value = notifications.value.map { it.copy(isActive = false, isRead = true) }
        }
        override suspend fun markRead(deviceId: String, notificationKey: String) {
            notifications.value = notifications.value.map { it.copy(isRead = true) }
        }
        override suspend fun upsert(notification: SyncedNotification) = error("Unused")
        override suspend fun markRemoved(deviceId: String, notificationKey: String, removedAt: Long) = error("Unused")
        override suspend fun reconcileActive(deviceId: String, activeKeys: List<String>, capturedAt: Long) = error("Unused")
        override suspend fun clear() = error("Unused")
        override suspend fun clearHistoryBefore(olderThanEpochMillis: Long) = error("Unused")
    }

    private class FakeServer(val gate: CompletableDeferred<Unit>? = null) : ClipboardServer {
        val requests = mutableListOf<NotificationActionRequest>()
        override suspend fun sendNotificationAction(action: NotificationActionRequest): Result<Unit> {
            requests += action
            gate?.await()
            return Result.failure(IllegalStateException("Android device is offline"))
        }
        override fun start(): Pair<StateFlow<ServerState>, SharedFlow<ServerEvent>> = error("Unused")
        override fun stop() = Unit
        override fun startPairing(): StateFlow<PairingState> = error("Unused")
        override suspend fun sendCopiedDataWebSocket(message: CopiedData): Result<Unit> = error("Unused")
        override fun observeServerState(): StateFlow<ServerState> = error("Unused")
        override fun observeServerEvent(): SharedFlow<ServerEvent> = error("Unused")
        override fun observePairingState(): StateFlow<PairingState?> = error("Unused")
    }
}
