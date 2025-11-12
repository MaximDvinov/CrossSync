package com.cross.sync.clipboard.data

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ClipboardManager {
    fun observeData(): StateFlow<CopiedData?>
    suspend fun setData(data: CopiedData)
    suspend fun getData(): CopiedData?
    fun init() : StateFlow<CopiedData?>
}