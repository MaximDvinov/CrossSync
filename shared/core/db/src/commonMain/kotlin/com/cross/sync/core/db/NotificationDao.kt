package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cross.sync.core.db.entities.SyncedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: SyncedNotificationEntity)

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
