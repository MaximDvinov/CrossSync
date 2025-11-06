package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentCopiedDataUseCase(
    private val repository: SystemClipboardRepository,
) {
    operator fun invoke(): Flow<CopiedData?> {
        return repository.observeData()
    }
}