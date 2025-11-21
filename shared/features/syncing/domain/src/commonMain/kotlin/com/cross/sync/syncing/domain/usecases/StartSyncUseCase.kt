package com.cross.sync.syncing.domain.usecases

import com.cross.sync.syncing.domain.entity.ClipboardMessage
import com.cross.sync.syncing.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow

class StartSyncUseCase(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke() {
        syncRepository.startSyncWithDb()
    }
}