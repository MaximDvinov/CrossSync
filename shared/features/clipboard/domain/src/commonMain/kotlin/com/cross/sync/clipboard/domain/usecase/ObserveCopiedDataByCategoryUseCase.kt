package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveCopiedDataByCategoryUseCase(
    private val repository: LocalClipboardRepository,
) {
    operator fun invoke(categoryId: Long): Flow<List<CopiedData>> {
        return repository.observeCopiedDataByCategory(categoryId)
    }
}