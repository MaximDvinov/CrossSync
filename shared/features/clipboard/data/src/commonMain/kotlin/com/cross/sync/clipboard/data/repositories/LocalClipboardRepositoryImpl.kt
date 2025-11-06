package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.collections.mutableSetOf
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalClipboardRepositoryImpl : LocalClipboardRepository {
    val dataFlow = MutableStateFlow<PersistentSet<CopiedData>>(persistentHashSetOf())

    override suspend fun addCopiedData(copiedData: CopiedData) {
        dataFlow.value = dataFlow.value.add(copiedData)
    }

    override suspend fun getLastCopiedData(): CopiedData {
        return dataFlow.value.last()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getCopiedDataById(id: Uuid): CopiedData? {
        return dataFlow.value.find { it.id == id }
    }

    override suspend fun deleteCopiedData(copiedData: CopiedData) {
        dataFlow.value = dataFlow.value.remove(copiedData)
    }

    override suspend fun getAllCopiedData(): List<CopiedData> {
        return dataFlow.value.toList()
    }

    override suspend fun clearAllCopiedData() {
        dataFlow.value = persistentHashSetOf()
    }

    @OptIn(ExperimentalTime::class)
    override fun observeCopiedData(): Flow<List<CopiedData>> {
        return dataFlow.map { data -> println("${data.size}"); data.toList().sortedBy { it.date } }
    }
}