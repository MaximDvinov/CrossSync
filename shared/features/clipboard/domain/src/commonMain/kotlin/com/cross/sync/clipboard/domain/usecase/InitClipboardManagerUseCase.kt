package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository

class InitClipboardManagerUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
    private val systemClipboardRepository: SystemClipboardRepository,
) {
    suspend operator fun invoke() {
        systemClipboardRepository.initClipboardManager().collect { data ->
            data?.let { copiedData -> localClipboardRepository.addCopiedData(copiedData) }
        }
    }
}