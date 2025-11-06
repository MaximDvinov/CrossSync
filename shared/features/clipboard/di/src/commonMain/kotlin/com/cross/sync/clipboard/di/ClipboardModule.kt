package com.cross.sync.clipboard.di

import com.cross.sync.clipboard.data.repositories.LocalClipboardRepositoryImpl
import com.cross.sync.clipboard.data.repositories.SystemClipboardRepositoryImpl
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.GetCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCurrentCopiedDataUseCase
import com.cross.sync.clipboard.presentation.ClipboardViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val clipboardModule: Module = module {
    singleOf(::SystemClipboardRepositoryImpl) bind SystemClipboardRepository::class
    singleOf(::LocalClipboardRepositoryImpl) bind LocalClipboardRepository::class

    singleOf(::AddCopiedDataUseCase)
    singleOf(::InitClipboardManagerUseCase)
    singleOf(::ObserveCopiedDataUseCase)
    singleOf(::ObserveCurrentCopiedDataUseCase)
    singleOf(::GetCopiedDataUseCase)

    factoryOf(::ClipboardViewModel)
}