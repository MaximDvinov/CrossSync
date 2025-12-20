package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository

class GetCopiedDataByCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(categoryId: Long) = localClipboardRepository.observeCategories()
}