package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategoryUseCase(
    private val repository: LocalClipboardRepository,
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.observeCategories()
    }
}