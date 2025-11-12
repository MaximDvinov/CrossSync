package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.data.mappers.toDomain
import com.cross.sync.clipboard.data.mappers.toEntity
import com.cross.sync.clipboard.db.AppDatabase
import com.cross.sync.clipboard.db.ClipboardDao
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalClipboardRepositoryImpl(
    private val dao: ClipboardDao,
) : LocalClipboardRepository {
    override suspend fun addCopiedData(copiedData: CopiedData) {
        dao.insert(copiedData = copiedData.toEntity())
    }

    override suspend fun getLastCopiedData(): CopiedData? {
        return dao.getLastCopiedData()?.toDomain()
    }

    override suspend fun getCopiedDataById(id: Int): CopiedData? {
        return dao.getById(id = id)?.toDomain()
    }

    override suspend fun deleteCopiedDataById(id: Int) {
        dao.deleteById(id)
    }

    override suspend fun getAllCopiedData(): List<CopiedData> {
        return dao.getAllCopiedData().map { it.toDomain() }
    }

    override suspend fun clearAllCopiedData() {
        dao.deleteAll()
    }

    @OptIn(ExperimentalTime::class)
    override fun observeCopiedData(): Flow<List<CopiedData>> {
        return dao.getAllCopiedDataFlow().map { data ->
            data.map { it.toDomain() }
        }
    }
}