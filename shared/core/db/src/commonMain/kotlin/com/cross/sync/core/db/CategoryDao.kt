package com.cross.sync.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cross.sync.core.db.entities.CategoryCopiedDataCrossRef
import com.cross.sync.core.db.entities.CategoryEntity
import com.cross.sync.core.db.entities.CategoryWithCopiedData
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM category WHERE categoryId = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Transaction
    @Query("SELECT * FROM category WHERE categoryId = :id LIMIT 1")
    fun getAllCopiedByCategoryDataFlow(id: Long): Flow<CategoryWithCopiedData>

    @Transaction
    @Query("SELECT * FROM category WHERE  categoryId = :id LIMIT 1")
    suspend fun getAllCopiedByCategoryData(id: Long): CategoryWithCopiedData

    @Query("SELECT * FROM category")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM category WHERE categoryId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM category")
    suspend fun deleteAll()

    @Insert(onConflict = IGNORE)
    suspend fun insert(copiedData: CategoryCopiedDataCrossRef)
}