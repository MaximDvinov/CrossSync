package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow

interface LocalClipboardRepository {
    suspend fun addCopiedData(copiedData: CopiedData)
    suspend fun getLastCopiedData(): CopiedData?
    suspend fun getCopiedDataById(id: Long): CopiedData?
    suspend fun deleteCopiedDataById(id: Long)

    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun getCategoryById(id: Long): Category?
    suspend fun deleteCategoryById(id: Long)

    suspend fun addCopiedDataToCategory(copiedDataId: Long, categoryId: Long)

    suspend fun getAllCopiedData(): List<CopiedData>
    suspend fun clearAllCopiedDataInCategory(categoryId: Long?)
    suspend fun clearUncategorizedCopiedData()
    suspend fun clearCopiedDataOlderThan(olderThanEpochMillis: Long)

    fun observeCopiedData(): Flow<List<CopiedData>>
    fun observeCopiedDataByCategory(categoryId: Long): Flow<List<CopiedData>>
    fun observeUncategorizedCopiedData(): Flow<List<CopiedData>>
    fun observeCategories(): Flow<List<Category>>
}
