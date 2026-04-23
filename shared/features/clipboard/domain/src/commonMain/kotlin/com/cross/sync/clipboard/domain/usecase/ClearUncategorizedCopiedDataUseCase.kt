package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class ClearUncategorizedCopiedDataUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke() {
        localClipboardRepository.clearUncategorizedCopiedData()
    }
}
