package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class RenameCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(category: Category) {
        localClipboardRepository.updateCategory(category)
    }
}