package com.cross.sync.syncing.domain.usecases

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.extractAuthorizationCode
import kotlinx.coroutines.CancellationException

/** Owned by the sequential incoming-event collector, never by a UI or database observer. */
internal class NotificationAuthorizationCodeCopier(
    private val clipboard: SystemClipboardRepository,
) {
    private data class CopyKey(val deviceId: String, val notificationKey: String, val code: String)
    private val copied = linkedMapOf<CopyKey, Long>()

    suspend fun copyIfPresent(
        notification: SyncedNotification,
        now: Long,
        suppressOutbound: (CopiedData.Text) -> Unit,
    ) {
        if (!notification.isActive || !notification.hasPreview || notification.isGroupSummary) return
        if (now - notification.postedAt !in -60_000L..MAX_CODE_AGE_MS) return
        val code = notification.authorizationCode
            ?: extractAuthorizationCode(notification.title, notification.body)
            ?: return
        // Validate the transport field as well: the clipboard receives a single token only.
        if (!Regex("[A-Za-z0-9]{4,10}").matches(code)) return
        copied.entries.removeAll { now - it.value > MAX_CODE_AGE_MS }
        val key = CopyKey(notification.deviceId, notification.notificationKey, code)
        if (key in copied) return

        val data = CopiedData.Text(id = 0, text = code, applicationId = notification.packageName)
        suppressOutbound(data)
        try {
            clipboard.setData(data)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A temporarily unavailable OS clipboard must not stop notification synchronization.
            return
        }
        copied[key] = now
        if (copied.size > 256) copied.remove(copied.keys.first())
    }

    private companion object {
        const val MAX_CODE_AGE_MS = 5 * 60_000L
    }
}
