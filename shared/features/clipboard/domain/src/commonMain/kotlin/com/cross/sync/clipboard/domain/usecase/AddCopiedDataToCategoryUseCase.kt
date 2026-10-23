package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class AddCopiedDataToCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(categoryId: Long, copiedDataId: Long) {
        localClipboardRepository.addCopiedDataToCategory(copiedDataId, categoryId)
    }
    suspend fun move(copiedDataId: Long, fromCategoryId: Long?, toCategoryId: Long) {
        localClipboardRepository.moveCopiedDataToCategory(copiedDataId, fromCategoryId, toCategoryId)
    }
}
