package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface LocalClipboardRepository {
    suspend fun addCopiedData(copiedData: CopiedData)
    suspend fun getLastCopiedData(): CopiedData?
    suspend fun getCopiedDataById(id: Int): CopiedData?
    suspend fun deleteCopiedDataById(id: Int)

    suspend fun getAllCopiedData(): List<CopiedData>
    suspend fun clearAllCopiedData()

    fun observeCopiedData(): Flow<List<CopiedData>>
}