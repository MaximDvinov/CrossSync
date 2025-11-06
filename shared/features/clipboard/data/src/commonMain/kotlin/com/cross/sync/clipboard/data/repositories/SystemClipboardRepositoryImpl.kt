package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlinx.coroutines.flow.Flow

class SystemClipboardRepositoryImpl(
    private val clipboardManager: ClipboardManager
) : SystemClipboardRepository {
    override suspend fun setData(copiedData: CopiedData) {
        clipboardManager.setData(copiedData)
    }

    override suspend fun getData() {
        clipboardManager.getData()
    }

    override fun observeData(): Flow<CopiedData?> {
        return clipboardManager.observeData()
    }
}