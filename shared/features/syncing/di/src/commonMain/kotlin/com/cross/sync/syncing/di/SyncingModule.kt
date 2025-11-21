package com.cross.sync.syncing.di

import com.cross.sync.syncing.domain.usecases.StartSyncUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformSyncingModule: Module

val syncingModule = module {
    includes(platformSyncingModule)
    singleOf(::StartSyncUseCase)
}