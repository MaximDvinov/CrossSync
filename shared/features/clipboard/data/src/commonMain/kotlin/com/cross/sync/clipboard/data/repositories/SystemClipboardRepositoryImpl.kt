package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class SystemClipboardRepositoryImpl(
    private val clipboardManager: ClipboardManager
) : SystemClipboardRepository {
    override suspend fun setData(copiedData: CopiedData) {
        clipboardManager.setData(copiedData)
    }

    override suspend fun getData(): CopiedData? {
        return clipboardManager.getData()
    }

    override fun observeData(): StateFlow<CopiedData?> {
        return clipboardManager.observeData()
    }

    override fun initClipboardManager(): StateFlow<CopiedData?>{
        return clipboardManager.init()
    }
}
