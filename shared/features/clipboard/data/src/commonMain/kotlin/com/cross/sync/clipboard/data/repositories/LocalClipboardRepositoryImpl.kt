package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalClipboardRepositoryImpl : LocalClipboardRepository {
    val dataFlow = MutableStateFlow<List<CopiedData>>(emptyList())

    override suspend fun addCopiedData(copiedData: CopiedData) {
        dataFlow.value += copiedData
    }

    override suspend fun getLastCopiedData(): CopiedData {
        return dataFlow.value.last()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getCopiedDataById(id: Uuid): CopiedData? {
        return dataFlow.value.find { it.id == id }
    }

    override suspend fun deleteCopiedData(copiedData: CopiedData) {
        dataFlow.value -= copiedData
    }

    override suspend fun getAllCopiedData(): List<CopiedData> {
        return dataFlow.value
    }

    override suspend fun clearAllCopiedData() {
        dataFlow.value = emptyList()
    }

    override fun observeCopiedData(): Flow<List<CopiedData>> {
        return dataFlow
    }
}