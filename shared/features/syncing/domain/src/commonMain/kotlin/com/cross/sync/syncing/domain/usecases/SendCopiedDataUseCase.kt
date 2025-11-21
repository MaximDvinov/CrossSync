package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.ClipboardMessage
import com.cross.sync.syncing.domain.repository.SyncRepository

class SendCopiedDataUseCase(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(message: ClipboardMessage): Result<Unit> {
        return syncRepository.sendCopiedData(message)
    }
}
