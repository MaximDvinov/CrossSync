package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InitClipboardManagerUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
    private val systemClipboardRepository: SystemClipboardRepository,
) {
    private val collectionMutex = Mutex()

    suspend operator fun invoke(): Unit = collectionMutex.withLock {
        systemClipboardRepository.initClipboardManager().collect { data ->
            data?.let { copiedData ->
                localClipboardRepository.addCopiedData(copiedData)
            }
        }
    }
}
