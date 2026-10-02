package com.cross.sync.notifications.domain

import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import com.cross.sync.notifications.domain.usecase.SynchronizeNotificationSnapshotsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SynchronizeNotificationSnapshotsUseCaseTest {
    @Test
    fun refreshesWhileConnectedAndRestartsFullSnapshotOnReconnect() = runTest {
        val connected = MutableStateFlow(false)
        val calls = mutableListOf<Boolean>()
        val publisher = object : NotificationSnapshotPublisher {
            override suspend fun publishActive(force: Boolean): Result<Unit> {
                calls += force
                return Result.success(Unit)
            }
        }
        backgroundScope.launch {
            SynchronizeNotificationSnapshotsUseCase(publisher)(connected) { throw it }
        }
        runCurrent()
        assertTrue(calls.isEmpty())
        connected.value = true
        runCurrent()
        assertEquals(listOf(true), calls)
        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(listOf(true, false), calls)
        connected.value = false
        runCurrent()
        advanceTimeBy(45_000)
        runCurrent()
        assertEquals(2, calls.size)
        connected.value = true
        runCurrent()
        assertEquals(listOf(true, false, true), calls)
    }

    @Test
    fun failedPublicationAndUnavailableListenerAreRetriedWithoutReconnect() = runTest {
        val calls = mutableListOf<Boolean>()
        val failures = mutableListOf<String?>()
        val publisher = object : NotificationSnapshotPublisher {
            override suspend fun publishActive(force: Boolean): Result<Unit> {
                calls += force
                return when (calls.size) {
                    1 -> Result.failure(IllegalStateException("Listener unavailable"))
                    2 -> throw IllegalStateException("Request failed")
                    else -> Result.success(Unit)
                }
            }
        }
        backgroundScope.launch {
            SynchronizeNotificationSnapshotsUseCase(publisher)(MutableStateFlow(true)) {
                failures += it.message
            }
        }
        runCurrent()
        repeat(3) {
            advanceTimeBy(15_000)
            runCurrent()
        }
        assertEquals(listOf<String?>("Listener unavailable", "Request failed"), failures)
        assertEquals(listOf(true, true, true, false), calls)
    }

    @Test
    fun disconnectCancelsInFlightPublicationWithoutTreatingItAsFailure() = runTest {
        val connected = MutableStateFlow(true)
        var publishing = false
        var cancelled = false
        var reportedFailure = false
        val publisher = object : NotificationSnapshotPublisher {
            override suspend fun publishActive(force: Boolean): Result<Unit> {
                publishing = true
                try {
                    awaitCancellation()
                } catch (error: CancellationException) {
                    cancelled = true
                    throw error
                } finally {
                    publishing = false
                }
            }
        }
        backgroundScope.launch {
            SynchronizeNotificationSnapshotsUseCase(publisher)(connected) { reportedFailure = true }
        }
        runCurrent()
        assertTrue(publishing)
        connected.value = false
        runCurrent()
        assertFalse(publishing)
        assertTrue(cancelled)
        assertFalse(reportedFailure)
    }
}
