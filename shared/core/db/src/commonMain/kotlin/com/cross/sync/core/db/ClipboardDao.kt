package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import com.cross.sync.core.db.entities.CopiedDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Insert(onConflict = IGNORE)
    suspend fun insert(copiedData: CopiedDataEntity)

    @Query("SELECT * FROM copied_data WHERE copiedDataId = :id")
    suspend fun getById(id: Long): CopiedDataEntity?

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLastCopiedData(): CopiedDataEntity?

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC")
    fun getAllCopiedDataFlow(): Flow<List<CopiedDataEntity>>

    @Query(
        """
        SELECT * FROM copied_data cd
        WHERE NOT EXISTS (
            SELECT 1 FROM CategoryCopiedDataCrossRef c
            WHERE c.copiedDataId = cd.copiedDataId
        )
        ORDER BY dateTime DESC
        """
    )
    fun getUncategorizedCopiedDataFlow(): Flow<List<CopiedDataEntity>>

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC")
    suspend fun getAllCopiedData(): List<CopiedDataEntity>

    @Query("DELETE FROM copied_data WHERE copiedDataId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM copied_data")
    suspend fun deleteAll()

    @Query("DELETE FROM copied_data WHERE dateTime < :olderThanEpochMillis")
    suspend fun deleteOlderThan(olderThanEpochMillis: Long)

    @Query(
        """
        DELETE FROM copied_data
        WHERE copiedDataId IN (
            SELECT cd.copiedDataId
            FROM copied_data cd
            WHERE NOT EXISTS (
                SELECT 1 FROM CategoryCopiedDataCrossRef c
                WHERE c.copiedDataId = cd.copiedDataId
            )
        )
        """
    )
    suspend fun deleteUncategorized()
}
