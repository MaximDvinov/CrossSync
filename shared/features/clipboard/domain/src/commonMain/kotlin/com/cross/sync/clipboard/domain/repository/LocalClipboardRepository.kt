package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface LocalClipboardRepository {
    suspend fun addCopiedData(copiedData: CopiedData)
    suspend fun getLastCopiedData(): CopiedData
    @OptIn(ExperimentalUuidApi::class)
    suspend fun getCopiedDataById(id: Uuid): CopiedData?
    suspend fun deleteCopiedData(copiedData: CopiedData)

    suspend fun getAllCopiedData(): List<CopiedData>
    suspend fun clearAllCopiedData()

    fun observeCopiedData(): Flow<List<CopiedData>>
}