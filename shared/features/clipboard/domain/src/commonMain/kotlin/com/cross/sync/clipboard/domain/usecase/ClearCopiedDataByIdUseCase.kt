@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlin.uuid.ExperimentalUuidApi

class ClearCopiedDataByIdUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(categoryId: Long?) {
        localClipboardRepository.clearAllCopiedDataInCategory(categoryId)
    }
}