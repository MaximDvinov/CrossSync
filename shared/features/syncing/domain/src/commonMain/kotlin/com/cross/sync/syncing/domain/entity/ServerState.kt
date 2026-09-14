package com.cross.sync.syncing.domain.entity

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification

sealed interface ServerState {
    class Starting : ServerState
    class Started : ServerState
    class Stopped : ServerState
    data class Error(
        val message: String, val throwable: Throwable
    ) : ServerState
}

sealed interface ServerEvent {
    data class AddedDevice(
        val deviceData: DeviceData
    ) : ServerEvent

    data class ReceivedCopiedData(
        val copiedData: CopiedData
    ) : ServerEvent

    data class ReceivedNotification(
        val deviceId: String,
        val notification: SyncedNotification,
    ) : ServerEvent

    data class RemovedNotification(
        val deviceId: String,
        val removal: NotificationRemoval,
    ) : ServerEvent
}
