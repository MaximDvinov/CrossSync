package com.cross.sync.notifications.domain.usecase

import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive

/** Runs in the sync service's lifetime; disconnecting cancels the current refresh. */
class SynchronizeNotificationSnapshotsUseCase(
    private val publisher: NotificationSnapshotPublisher,
) {
    suspend operator fun invoke(connected: Flow<Boolean>, onFailure: (Exception) -> Unit) {
        connected.distinctUntilChanged().collectLatest { isConnected ->
            if (!isConnected) return@collectLatest
            var force = true
            while (currentCoroutineContext().isActive) {
                try {
                    publisher.publishActive(force).getOrThrow()
                    force = false
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    onFailure(error)
                }
                delay(REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 15_000L
    }
}
