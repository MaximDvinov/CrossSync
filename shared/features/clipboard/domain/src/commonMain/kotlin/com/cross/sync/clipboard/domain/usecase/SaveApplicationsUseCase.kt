package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.repository.ApplicationRepository

class SaveApplicationsUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(list: List<Application>) {
        return repository.saveApplication(list)
    }
}