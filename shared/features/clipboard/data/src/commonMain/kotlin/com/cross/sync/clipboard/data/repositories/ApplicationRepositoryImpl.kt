package com.cross.sync.clipboard.data.repositories

import com.cross.sync.clipboard.data.mappers.toDomain
import com.cross.sync.clipboard.data.mappers.toEntity
import com.cross.sync.clipboard.db.ApplicationDao
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.repository.ApplicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ApplicationRepositoryImpl(
    private val applicationDao: ApplicationDao,
) : ApplicationRepository {
    override fun getApplications(): Flow<List<Application>> {
        return applicationDao.getApplications().map { flow -> flow.map { it.toDomain() } }
    }

    override suspend fun saveApplication(applications: List<Application>) {
        applicationDao.insertApplications(applications.map { it.toEntity() })
    }
}