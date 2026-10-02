package com.cross.sync.notifications.domain

import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.entity.currentLiveUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveUpdateNotificationTest {
    @Test
    fun incomingCallWithoutOngoingFlagHasPriorityOverNewerLiveUpdate() {
        val call = notification("call", NotificationKind.Call, updatedAt = 100)
        val progress = notification("progress", NotificationKind.Progress, updatedAt = 200).copy(isOngoing = true)
        assertEquals(call, listOf(call, progress).currentLiveUpdate())
        assertEquals(progress, listOf(call.copy(isActive = false), progress).currentLiveUpdate())
    }

    @Test
    fun endedCallAndOrdinaryNotificationsDoNotKeepLiveUpdateVisible() {
        assertNull(listOf(
            notification("call", NotificationKind.Call).copy(isActive = false),
            notification("message", NotificationKind.Message),
        ).currentLiveUpdate())
    }

    private fun notification(key: String, kind: NotificationKind, updatedAt: Long = 100) = SyncedNotification(
        notificationKey = key,
        packageName = "example",
        appName = "Example",
        title = "Title",
        body = "Body",
        kind = kind,
        postedAt = 50,
        updatedAt = updatedAt,
    )
}
