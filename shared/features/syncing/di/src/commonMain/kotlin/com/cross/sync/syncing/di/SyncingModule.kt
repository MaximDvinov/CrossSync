package com.cross.sync.syncing.di

import com.cross.sync.syncing.data.repository.DeviceRepositoryImpl
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.domain.usecases.ConnectToServerUseCase
import com.cross.sync.syncing.domain.usecases.PairToServerUseCase
import com.cross.sync.syncing.domain.usecases.SendCopiedDataUseCase
import com.cross.sync.syncing.network.ClipboardClient
import com.cross.sync.syncing.presentation.ConnectDeviceViewModel
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformSyncingModule: Module

val syncingModule = module {
    singleOf(::DeviceRepositoryImpl) bind DeviceRepository::class
    single<Settings> { Settings() }
    singleOf(::ClipboardClient)
    includes(platformSyncingModule)

    singleOf(::ConnectToServerUseCase)
    singleOf(::PairToServerUseCase)
    singleOf(::SendCopiedDataUseCase)

    viewModelOf(::ConnectDeviceViewModel)
}