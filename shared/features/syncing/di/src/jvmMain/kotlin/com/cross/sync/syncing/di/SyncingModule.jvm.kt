package com.cross.sync.syncing.di

import com.cross.sync.syncing.data.repository.SyncRepositoryImpl
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.repository.SyncRepository
import com.cross.sync.syncing.domain.usecases.AddDeviceUseCase
import com.cross.sync.syncing.domain.usecases.DeleteDeviceUseCase
import com.cross.sync.syncing.domain.usecases.ObserveDevicesUseCase
import com.cross.sync.syncing.domain.usecases.ObservePairingUseCase
import com.cross.sync.syncing.domain.usecases.StartSyncUseCase
import com.cross.sync.syncing.domain.usecases.SendNotificationActionUseCase
import com.cross.sync.syncing.network.LocalServer
import com.cross.sync.syncing.network.crypto.AesGcmCryptoEngine
import com.cross.sync.syncing.network.crypto.CryptoEngine
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformSyncingModule: Module
    get() = module {

        singleOf(::AesGcmCryptoEngine) bind CryptoEngine::class
        singleOf(::LocalServer) bind ClipboardServer::class
        singleOf(::SyncRepositoryImpl) bind SyncRepository::class
        singleOf(::AddDeviceUseCase)
        singleOf(::DeleteDeviceUseCase)
        singleOf(::ObserveDevicesUseCase)
        singleOf(::ObservePairingUseCase)
        singleOf(::StartSyncUseCase)
        singleOf(::SendNotificationActionUseCase)
    }
