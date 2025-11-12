package com.cross.sync.clipboard.domain.usecase

import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.repository.ApplicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class GetApplicationsUseCase(
    private val repository: ApplicationRepository,
) {
    operator fun invoke(): Flow<List<Application>> {
        return repository.getApplications()
    }
}