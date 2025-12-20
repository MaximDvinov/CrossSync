@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlin.uuid.ExperimentalUuidApi

class DeleteCategoryUseCase(
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(id: Long) {
        localClipboardRepository.deleteCategoryById(id)
    }
}