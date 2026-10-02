package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cross.sync.core.db.entities.SyncedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: SyncedNotificationEntity)

    @Query("SELECT * FROM synced_notifications WHERE deviceId = :deviceId AND notificationKey = :notificationKey")
    suspend fun get(deviceId: String, notificationKey: String): SyncedNotificationEntity?

    @Transaction
    suspend fun upsertFromDevice(notification: SyncedNotificationEntity) {
        val previous = get(notification.deviceId, notification.notificationKey)
        // Reconnect snapshots must not resurrect locally dismissed/read notifications
        // or produce another banner when only the transport timestamp changed.
        fun SyncedNotificationEntity.content() = copy(updatedAt = 0, isActive = true, isRead = false)
        if (previous != null && previous.content() == notification.content()) return
        upsert(notification)
    }

    @Query("UPDATE synced_notifications SET isActive = 0, isRead = 1 WHERE deviceId = :deviceId AND notificationKey = :notificationKey")
    suspend fun dismiss(deviceId: String, notificationKey: String)

    @Query("UPDATE synced_notifications SET isActive = 0 WHERE deviceId = :deviceId AND notificationKey NOT IN (:activeKeys) AND updatedAt <= :capturedAt")
    suspend fun reconcileActive(deviceId: String, activeKeys: List<String>, capturedAt: Long)

    @Query("SELECT * FROM synced_notifications ORDER BY isActive DESC, updatedAt DESC")
    fun observeAll(): Flow<List<SyncedNotificationEntity>>

    @Query("UPDATE synced_notifications SET isActive = 0, updatedAt = :updatedAt WHERE deviceId = :deviceId AND notificationKey = :notificationKey")
    suspend fun markRemoved(deviceId: String, notificationKey: String, updatedAt: Long)

    @Query("UPDATE synced_notifications SET isRead = 1 WHERE deviceId = :deviceId AND notificationKey = :notificationKey")
    suspend fun markRead(deviceId: String, notificationKey: String)

    @Query("DELETE FROM synced_notifications")
    suspend fun clear()

    @Query("DELETE FROM synced_notifications WHERE isActive = 0 AND updatedAt < :olderThanEpochMillis")
    suspend fun clearHistoryBefore(olderThanEpochMillis: Long)
}
