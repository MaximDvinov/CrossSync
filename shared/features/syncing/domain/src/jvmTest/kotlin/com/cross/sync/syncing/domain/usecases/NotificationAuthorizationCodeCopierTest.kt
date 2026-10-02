package com.cross.sync.syncing.domain.usecases

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.notifications.domain.entity.SyncedNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationAuthorizationCodeCopierTest {
    private val now = 1_000_000L

    @Test
    fun copiesOnlyCodeAndSuppressesEchoBeforeUpdatingClipboard() = runBlocking {
        val clipboard = FakeClipboard()
        val copier = NotificationAuthorizationCodeCopier(clipboard)
        var suppressed: String? = null
        clipboard.beforeWrite = { assertEquals("012345", suppressed) }
        copier.copyIfPresent(notification(body = "Код подтверждения: 012345"), now) {
            suppressed = it.text
        }
        assertEquals(listOf("012345"), clipboard.writes)
    }

    @Test
    fun duplicateUpdatesDoNotOverwriteLaterUserClipboardButNewCodesDo() = runBlocking {
        val clipboard = FakeClipboard()
        val copier = NotificationAuthorizationCodeCopier(clipboard)
        val message = notification()
        copier.copyIfPresent(message, now) {}
        copier.copyIfPresent(message.copy(updatedAt = now + 1, postedAt = now + 1), now + 1) {}
        copier.copyIfPresent(message.copy(body = "OTP: 654321"), now + 2) {}
        copier.copyIfPresent(message.copy(deviceId = "another-phone"), now + 3) {}
        assertEquals(listOf("123456", "654321", "123456"), clipboard.writes)
    }

    @Test
    fun skipsSnapshotsHiddenContentOldNotificationsAndSummaries() = runBlocking {
        val clipboard = FakeClipboard()
        val copier = NotificationAuthorizationCodeCopier(clipboard)
        val message = notification()
        listOf(
            message.copy(authorizationCode = ""),
            message.copy(hasPreview = false),
            message.copy(isActive = false),
            message.copy(isGroupSummary = true),
            message.copy(postedAt = now - 300_001),
            message.copy(postedAt = now + 60_001),
            message.copy(authorizationCode = "Code: 123456"),
        ).forEach { copier.copyIfPresent(it, now) {} }
        assertTrue(clipboard.writes.isEmpty())
    }

    @Test
    fun usesCurrentSourceCodeInsteadOfOldPreviewAndSupportsLegacySenders() = runBlocking {
        val clipboard = FakeClipboard()
        val copier = NotificationAuthorizationCodeCopier(clipboard)
        copier.copyIfPresent(notification().copy(authorizationCode = "654321"), now) {}
        copier.copyIfPresent(notification().copy(authorizationCode = ""), now) {}
        copier.copyIfPresent(notification().copy(notificationKey = "legacy"), now) {}
        assertEquals(listOf("654321", "123456"), clipboard.writes)
    }

    @Test
    fun clipboardFailureAllowsRetryAndCancellationPropagates() = runBlocking {
        val clipboard = FakeClipboard()
        val copier = NotificationAuthorizationCodeCopier(clipboard)
        clipboard.beforeWrite = { error("Clipboard unavailable") }
        copier.copyIfPresent(notification(), now) {}
        clipboard.beforeWrite = {}
        copier.copyIfPresent(notification(), now) {}
        assertEquals(listOf("123456"), clipboard.writes)
        clipboard.beforeWrite = { throw CancellationException() }
        assertFailsWith<CancellationException> {
            copier.copyIfPresent(notification().copy(notificationKey = "cancelled"), now) {}
        }
        Unit
    }

    private fun notification(body: String = "OTP: 123456") = SyncedNotification(
        deviceId = "phone", notificationKey = "message", packageName = "any.push.app",
        appName = "Any app", title = "", body = body, postedAt = now, updatedAt = now,
    )

    private class FakeClipboard : SystemClipboardRepository {
        val writes = mutableListOf<String>()
        var beforeWrite: () -> Unit = {}
        private val state = MutableStateFlow<CopiedData?>(null)
        override suspend fun setData(copiedData: CopiedData) {
            beforeWrite()
            writes += (copiedData as CopiedData.Text).text
            state.value = copiedData
        }
        override suspend fun getData(): CopiedData? = state.value
        override fun observeData(): StateFlow<CopiedData?> = state
        override fun initClipboardManager(): StateFlow<CopiedData?> = state
    }
}
