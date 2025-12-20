package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class AddCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(category: Category) {
        localClipboardRepository.addCategory(category)
    }
}