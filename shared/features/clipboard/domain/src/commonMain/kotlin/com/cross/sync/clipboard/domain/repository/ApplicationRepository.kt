package com.cross.sync.clipboard.domain.repository

import com.cross.sync.clipboard.domain.entity.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ApplicationRepository {
    fun getApplications(): Flow<List<Application>>
    suspend fun saveApplication(applications: List<Application>)
}