package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class ClearCopiedDataOlderThanUseCase(
    private val localClipboardRepository: LocalClipboardRepository
) {
    suspend operator fun invoke(olderThanEpochMillis: Long) {
        localClipboardRepository.clearCopiedDataOlderThan(olderThanEpochMillis)
    }
}
