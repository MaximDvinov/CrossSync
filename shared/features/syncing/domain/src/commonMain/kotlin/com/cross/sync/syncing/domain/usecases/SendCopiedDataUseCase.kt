package com.cross.sync.syncing.domain.usecases

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.syncing.domain.repository.SyncRepository

class SendCopiedDataUseCase(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(
        copiedData: CopiedData
    ): Result<Unit> {
        return syncRepository.sendCopiedData(copiedData)
    }
}
