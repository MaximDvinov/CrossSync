package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository

class GetCopiedDataUseCase(
    private val systemClipboardRepository: SystemClipboardRepository,
) {
    suspend operator fun invoke(): CopiedData? {
        return systemClipboardRepository.getData()
    }
}
