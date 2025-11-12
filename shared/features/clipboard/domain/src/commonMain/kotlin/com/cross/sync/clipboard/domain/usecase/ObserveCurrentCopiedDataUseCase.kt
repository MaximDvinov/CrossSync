package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveCurrentCopiedDataUseCase(
    private val repository: SystemClipboardRepository,
) {
    operator fun invoke(): StateFlow<CopiedData?> {
        return repository.observeData()
    }
}