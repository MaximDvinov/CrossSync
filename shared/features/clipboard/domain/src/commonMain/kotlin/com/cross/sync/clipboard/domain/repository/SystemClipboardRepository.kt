package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow

interface SystemClipboardRepository {
    suspend fun setData(copiedData: CopiedData)
    suspend fun getData()
    fun observeData(): Flow<CopiedData?>
}