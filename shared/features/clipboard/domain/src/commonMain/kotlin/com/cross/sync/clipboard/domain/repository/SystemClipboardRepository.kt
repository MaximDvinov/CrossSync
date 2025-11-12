package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SystemClipboardRepository {
    suspend fun setData(copiedData: CopiedData)
    suspend fun getData()
    fun observeData(): StateFlow<CopiedData?>
    fun initClipboardManager(): StateFlow<CopiedData?>
}