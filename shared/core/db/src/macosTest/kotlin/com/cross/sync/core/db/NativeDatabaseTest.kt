@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cross.sync.core.db

import com.cross.sync.core.db.entities.CopiedDataEntity
import com.cross.sync.core.db.entities.CopiedDataType
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.onEach
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class NativeDatabaseTest {
    @Test
    fun observersReceiveCommittedWrites() = runTest(timeout = 15.seconds) {
        val directory = NSTemporaryDirectory() + "CrossSyncRoomFlowTest-${kotlin.random.Random.nextLong()}"
        val database = createNativeDatabase(directory)
        try {
            val initial = CompletableDeferred<Unit>()
            val observer = async {
                database.getClipboardDao().getAllCopiedDataFlow().onEach { initial.complete(Unit) }.first { it.isNotEmpty() }
            }
            initial.await()
            database.getClipboardDao().insert(CopiedDataEntity(123, CopiedDataType.TEXT, 1, null, "fixture"))
            assertEquals("fixture", observer.await().single().content)
        } finally { database.close() }
    }
    @Test
    fun concurrentObserversAndWritersInitializeOnce() = runTest(timeout = 15.seconds) {
        val directory = NSTemporaryDirectory() + "CrossSyncRoomConcurrentTest-${kotlin.random.Random.nextLong()}"
        val database = createNativeDatabase(directory)
        try {
            val operations = listOf(
                async { database.getApplicationDao().insertApplications(emptyList()) },
                async { database.getClipboardDao().insert(CopiedDataEntity(123, CopiedDataType.TEXT, 1, null, "fixture")) },
                async { database.getClipboardDao().getAllCopiedDataFlow().first() },
                async { database.getNotificationDao().observeAll().first() },
            )
            operations.awaitAll()
            assertEquals("fixture", database.getClipboardDao().getById(123)?.content)
        } finally { database.close() }
    }
    @Test
    fun writeReadAndReopenPreserveSchema() = runTest(timeout = 15.seconds) {
        val directory = NSTemporaryDirectory() + "CrossSyncRoomTest-${kotlin.random.Random.nextLong()}"
        val database = createNativeDatabase(directory)
        try {
            database.getClipboardDao().insert(CopiedDataEntity(123, CopiedDataType.TEXT, 1, null, "fixture"))
            assertEquals("fixture", database.getClipboardDao().getById(123)?.content)
        } finally { database.close() }
        val reopened = createNativeDatabase(directory)
        try { assertEquals("fixture", reopened.getClipboardDao().getById(123)?.content) }
        finally { reopened.close() }
    }
}
