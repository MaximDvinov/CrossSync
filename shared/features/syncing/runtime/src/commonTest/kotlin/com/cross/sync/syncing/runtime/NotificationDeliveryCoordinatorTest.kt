package com.cross.sync.syncing.runtime

import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.setting.domain.DesktopNotificationDelivery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class NotificationDeliveryCoordinatorTest {
    @Test
    fun liveUpdateRestoresAndClearsWithoutASeparateHistoryObserver() {
        var current: SyncedNotification? = null
        val coordinator = NotificationDeliveryCoordinator(
            { DesktopNotificationDelivery.BOTH }, {}, {}, {}, liveUpdate = { current = it },
        )
        val call = notification("call", 2).copy(kind = NotificationKind.Call)
        coordinator.onChanged(listOf(call))
        assertEquals(call, current)
        coordinator.onChanged(listOf(call.copy(isActive = false)))
        assertNull(current)
    }

    @Test
    fun restoredHistoryIsSilentUpdatesDeliverOnceAndRemovalWithdraws() {
        val system = mutableListOf<SyncedNotification>()
        val popup = mutableListOf<SyncedNotification>()
        val hidden = mutableListOf<String>()
        val coordinator = NotificationDeliveryCoordinator({ DesktopNotificationDelivery.BOTH }, system::add, popup::add, hidden::add)
        val restored = notification("old", 1)
        coordinator.onChanged(listOf(restored))
        assertTrue(system.isEmpty())
        val fresh = notification("new", 2)
        coordinator.onChanged(listOf(restored, fresh))
        coordinator.onChanged(listOf(restored, fresh))
        assertEquals(listOf(fresh), system)
        assertEquals(listOf(fresh), popup)
        coordinator.onChanged(listOf(restored, fresh.copy(updatedAt = 3)))
        assertEquals(2, system.size)
        coordinator.onChanged(listOf(restored))
        assertEquals(listOf("phone:new"), hidden)
    }

    @Test
    fun modesAndLiveUpdatesDoNotProduceUnexpectedBanners() {
        for (mode in DesktopNotificationDelivery.entries) {
            val system = mutableListOf<SyncedNotification>()
            val popup = mutableListOf<SyncedNotification>()
            val coordinator = NotificationDeliveryCoordinator({ mode }, system::add, popup::add, {})
            coordinator.onChanged(emptyList())
            val normal = notification("normal", 1)
            coordinator.onChanged(listOf(normal, notification("call", 2).copy(kind = NotificationKind.Call), notification("progress", 3).copy(isOngoing = true)))
            assertEquals(if (mode.showsSystem) listOf(normal) else emptyList(), system)
            assertEquals(if (mode.showsPopup) listOf(normal) else emptyList(), popup)
        }
    }

    private fun notification(key: String, version: Long) = SyncedNotification(
        deviceId = "phone", notificationKey = key, packageName = "app", appName = "App",
        title = "Title", body = "Body", postedAt = version, updatedAt = version,
    )
}
