package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository

class AddCopiedDataUseCase(
    private val systemClipboardRepository: SystemClipboardRepository,
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(data: CopiedData) {
        systemClipboardRepository.setData(data)
    }
}