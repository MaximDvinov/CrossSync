package com.cross.sync.syncing.di

import com.cross.sync.syncing.data.repository.DesktopSyncRepositoryImpl
import com.cross.sync.syncing.domain.repository.SyncRepository
import com.cross.sync.syncing.network.LocalServer
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformSyncingModule: Module
    get() = module {

        singleOf(::LocalServer)
        singleOf(::DesktopSyncRepositoryImpl) bind SyncRepository::class
    }