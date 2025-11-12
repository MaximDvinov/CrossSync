@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AddCopiedDataUseCase(
    private val systemClipboardRepository: SystemClipboardRepository,
    private val localClipboardRepository: LocalClipboardRepository,
) {
    suspend operator fun invoke(id: Int) {
        val data = localClipboardRepository.getCopiedDataById(id)
        data?.let { systemClipboardRepository.setData(it) }
    }
}