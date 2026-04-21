package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.cross.sync.core.db.entities.ApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertApplications(list: List<ApplicationEntity>)

    @Query("SELECT * FROM applications")
    fun getApplications(): Flow<List<ApplicationEntity>>
}