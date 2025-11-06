package com.cross.sync.clipboard.data

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow

interface ClipboardManager {
    fun observeData(): Flow<CopiedData?>
    suspend fun setData(data: CopiedData)
    suspend fun getData(): CopiedData?
}