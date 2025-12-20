package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class AddCopiedDataToCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(categoryId: Long, copiedDataId: Long) {
        localClipboardRepository.addCopiedDataToCategory(copiedDataId, categoryId)
    }
}