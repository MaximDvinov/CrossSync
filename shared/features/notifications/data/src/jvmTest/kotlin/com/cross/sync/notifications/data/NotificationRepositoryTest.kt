package com.cross.sync.notifications.data

import androidx.room.Room
import com.cross.sync.core.db.AppDatabase
import com.cross.sync.core.db.getRoomDatabase
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import com.cross.sync.notifications.domain.usecase.SynchronizeNotificationSnapshotsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationRepositoryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun missingRemovalEventIsRepairedWhileConnectionStaysOpen() = runTest {
        getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>()).withRepository { repository ->
            repository.upsert(notification())
            var phoneKeys = listOf("message")
            var capturedAt = 200L
            val publisher = object : NotificationSnapshotPublisher {
                override suspend fun publishActive(force: Boolean): Result<Unit> {
                    repository.reconcileActive("phone", phoneKeys, capturedAt)
                    return Result.success(Unit)
                }
            }
            val sync = backgroundScope.launch {
                SynchronizeNotificationSnapshotsUseCase(publisher)(MutableStateFlow(true)) { throw it }
            }
            runCurrent()
            assertTrue(repository.observeNotifications().first().single().isActive)

            // The phone removed/read the message, but its removal event was lost.
            phoneKeys = emptyList()
            capturedAt = 300
            advanceTimeBy(15_000)
            runCurrent()
            assertFalse(repository.observeNotifications().first().single().isActive)
            sync.cancel()
        }
    }

    @Test
    fun dismissalSurvivesReconnectAndDatabaseReopening() = runTest {
        val file = java.nio.file.Files.createTempFile("notification-test", ".db").toFile()
        file.delete()
        fun open() = getRoomDatabase(Room.databaseBuilder<AppDatabase>(file.absolutePath))
        try {
            open().withRepository { repository ->
                repository.upsert(notification())
                repository.dismiss("phone", "message")
            }
            open().withRepository { repository ->
                repository.upsert(notification().copy(updatedAt = 200))
                val stored = repository.observeNotifications().first().single()
                assertFalse(stored.isActive)
                assertTrue(stored.isRead)

                repository.upsert(notification().copy(body = "New message", updatedAt = 300))
                val updated = repository.observeNotifications().first().single()
                assertTrue(updated.isActive)
                assertFalse(updated.isRead)
                assertEquals("New message", updated.body)
            }
        } finally {
            file.delete()
            java.io.File(file.path + "-wal").delete()
            java.io.File(file.path + "-shm").delete()
        }
    }

    @Test
    fun repeatedSnapshotPreservesReadStateAndDoesNotCreateAnotherBannerVersion() = runTest {
        getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>()).withRepository { repository ->
            repository.upsert(notification())
            repository.markRead("phone", "message")
            repository.upsert(notification().copy(updatedAt = 200))
            val stored = repository.observeNotifications().first().single()
            assertTrue(stored.isRead)
            assertEquals(100L, stored.updatedAt)
        }
    }

    @Test
    fun reconnectSnapshotRemovesMissingEntriesOnlyForItsDeviceAndTime() = runTest {
        getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>()).withRepository { repository ->
            repository.upsert(notification())
            repository.upsert(notification().copy(notificationKey = "active"))
            repository.upsert(notification().copy(notificationKey = "newer", updatedAt = 300))
            repository.upsert(notification().copy(deviceId = "other-phone"))
            repository.reconcileActive("phone", listOf("active"), 200)
            val entries = repository.observeNotifications().first()
            assertFalse(entries.single { it.deviceId == "phone" && it.notificationKey == "message" }.isActive)
            assertTrue(entries.single { it.notificationKey == "active" }.isActive)
            assertTrue(entries.single { it.notificationKey == "newer" }.isActive)
            assertTrue(entries.single { it.deviceId == "other-phone" }.isActive)

            repository.reconcileActive("phone", emptyList(), 400)
            assertTrue(repository.observeNotifications().first().filter { it.deviceId == "phone" }.none { it.isActive })
        }
    }

    private suspend fun AppDatabase.withRepository(block: suspend (NotificationRepositoryImpl) -> Unit) {
        try {
            block(NotificationRepositoryImpl(getNotificationDao()))
        } finally {
            close()
        }
    }

    private fun notification() = SyncedNotification(
        deviceId = "phone",
        notificationKey = "message",
        packageName = "example",
        appName = "Example",
        title = "Message",
        body = "Hello",
        postedAt = 50,
        updatedAt = 100,
    )
}
