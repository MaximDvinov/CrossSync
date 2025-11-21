package com.cross.sync.clipboard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import com.cross.sync.clipboard.db.entities.CopiedDataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Dao
interface ClipboardDao {
    @Insert(onConflict = IGNORE)
    suspend fun insert(copiedData: CopiedDataEntity)

    @Query("SELECT * FROM copied_data WHERE id = :id")
    suspend fun getById(id: Int): CopiedDataEntity?

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLastCopiedData(): CopiedDataEntity?

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC")
    fun getAllCopiedDataFlow(): Flow<List<CopiedDataEntity>>

    @Query("SELECT * FROM copied_data ORDER BY dateTime DESC")
    suspend fun getAllCopiedData(): List<CopiedDataEntity>

    @Query("DELETE FROM copied_data WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM copied_data")
    suspend fun deleteAll()
}