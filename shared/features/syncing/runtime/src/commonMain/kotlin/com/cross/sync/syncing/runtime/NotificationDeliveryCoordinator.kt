package com.cross.sync.syncing.runtime

import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.entity.isLiveUpdate
import com.cross.sync.notifications.domain.entity.currentLiveUpdate
import com.cross.sync.setting.domain.DesktopNotificationDelivery

/** Shared delivery policy. Platform code only renders the resulting effects. */
class NotificationDeliveryCoordinator(
    private val delivery: () -> DesktopNotificationDelivery,
    private val system: (SyncedNotification) -> Unit,
    private val popup: (SyncedNotification) -> Unit,
    private val hide: (String) -> Unit,
    private val liveUpdate: (SyncedNotification?) -> Unit = {},
) {
    private var initialized = false
    private val delivered = mutableMapOf<String, Long>()
    fun onChanged(notifications: List<SyncedNotification>) {
        liveUpdate(notifications.currentLiveUpdate())
        val active = notifications.filter { it.isActive && !it.isLiveUpdate }
        val ids = active.mapTo(mutableSetOf()) { key(it) }
        if (initialized) {
            delivered.keys.filterNot(ids::contains).forEach(hide)
            val mode = delivery()
            active.filter { delivered[key(it)] != it.updatedAt }.forEach {
                if (mode.showsSystem) system(it)
                if (mode.showsPopup) popup(it)
            }
        }
        delivered.clear()
        active.forEach { delivered[key(it)] = it.updatedAt }
        initialized = true
    }
    private fun key(notification: SyncedNotification) = "${notification.deviceId}:${notification.notificationKey}"
}
