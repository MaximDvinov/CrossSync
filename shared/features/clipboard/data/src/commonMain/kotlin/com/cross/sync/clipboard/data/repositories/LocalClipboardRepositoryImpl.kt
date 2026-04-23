@file:OptIn(ExperimentalTime::class)

package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.data.mappers.toDomain
import com.cross.sync.clipboard.data.mappers.toEntity
import com.cross.sync.core.db.CategoryDao
import com.cross.sync.core.db.ClipboardDao
import com.cross.sync.core.db.entities.CategoryCopiedDataCrossRef
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LocalClipboardRepositoryImpl(
    private val clipboardDao: ClipboardDao,
    private val categoryDao: CategoryDao,
) : LocalClipboardRepository {
    override suspend fun addCopiedData(copiedData: CopiedData) {
        clipboardDao.insert(
            copiedData = copiedData.toEntity()
                .copy(dateTime = Clock.System.now().toEpochMilliseconds())
        )
    }

    override suspend fun getLastCopiedData(): CopiedData? {
        return clipboardDao.getLastCopiedData()?.toDomain()
    }

    override suspend fun getCopiedDataById(id: Long): CopiedData? {
        return clipboardDao.getById(id = id)?.toDomain()
    }

    override suspend fun deleteCopiedDataById(id: Long) {
        clipboardDao.deleteById(id)
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    override suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteById(id)
    }

    override suspend fun addCopiedDataToCategory(
        copiedDataId: Long,
        categoryId: Long,
    ) {
        categoryDao.insert(CategoryCopiedDataCrossRef(categoryId, copiedDataId))
    }

    override suspend fun clearAllCopiedDataInCategory(categoryId: Long?) {
        if (categoryId != null) {
            categoryDao.getAllCopiedByCategoryData(categoryId).copiedDataList.forEach {
                clipboardDao.deleteById(it.copiedDataId)
            }
        } else {
            clipboardDao.deleteAll()
        }

    }

    override suspend fun clearUncategorizedCopiedData() {
        clipboardDao.deleteUncategorized()
    }

    override suspend fun clearCopiedDataOlderThan(olderThanEpochMillis: Long) {
        clipboardDao.deleteOlderThan(olderThanEpochMillis)
    }

    override suspend fun getAllCopiedData(): List<CopiedData> {
        return clipboardDao.getAllCopiedData().map { it.toDomain() }
    }

    override fun observeCopiedData(): Flow<List<CopiedData>> {
        return clipboardDao.getAllCopiedDataFlow().map { data ->
            data.map { it.toDomain() }
        }
    }

    override fun observeCopiedDataByCategory(categoryId: Long): Flow<List<CopiedData>> {
        return categoryDao.getAllCopiedByCategoryDataFlow(categoryId)
            .map {
                it.copiedDataList.map { entity -> entity.toDomain() }
                    .sortedByDescending { entity -> entity.dateTime }
            }
    }

    override fun observeUncategorizedCopiedData(): Flow<List<CopiedData>> {
        return clipboardDao.getUncategorizedCopiedDataFlow().map { data ->
            data.map { it.toDomain() }
        }
    }

    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { data ->
            data.map { it.toDomain() }
        }
    }
}
