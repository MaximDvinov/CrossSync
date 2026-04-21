package com.cross.sync.clipboard.di

import com.cross.sync.clipboard.data.repositories.ApplicationRepositoryImpl
import com.cross.sync.clipboard.data.repositories.LocalClipboardRepositoryImpl
import com.cross.sync.clipboard.data.repositories.SystemClipboardRepositoryImpl
import com.cross.sync.core.db.databaseModule
import com.cross.sync.clipboard.domain.repository.ApplicationRepository
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.clipboard.domain.usecase.AddCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataToCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ClearCopiedDataByIdUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCopiedDataByIdUseCase
import com.cross.sync.clipboard.domain.usecase.GetApplicationsUseCase
import com.cross.sync.clipboard.domain.usecase.GetCopiedDataByCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.GetCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataByCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCurrentCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.RenameCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.SaveApplicationsUseCase
import com.cross.sync.clipboard.presentation.ClipboardViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val clipboardModule: Module = module {
    includes(databaseModule)

    singleOf(::SystemClipboardRepositoryImpl) bind SystemClipboardRepository::class
    singleOf(::LocalClipboardRepositoryImpl) bind LocalClipboardRepository::class
    singleOf(::ApplicationRepositoryImpl) bind ApplicationRepository::class

    singleOf(::AddCopiedDataUseCase)
    singleOf(::DeleteCopiedDataByIdUseCase)
    singleOf(::InitClipboardManagerUseCase)
    singleOf(::ObserveCopiedDataUseCase)
    singleOf(::ObserveCurrentCopiedDataUseCase)
    singleOf(::GetCopiedDataUseCase)
    singleOf(::ClearCopiedDataByIdUseCase)

    singleOf(::AddCategoryUseCase)
    singleOf(::DeleteCategoryUseCase)
    singleOf(::AddCopiedDataToCategoryUseCase)
    singleOf(::GetCopiedDataByCategoryUseCase)
    singleOf(::ObserveCopiedDataByCategoryUseCase)
    singleOf(::ObserveCategoryUseCase)
    singleOf(::RenameCategoryUseCase)

    singleOf(::GetApplicationsUseCase)
    singleOf(::SaveApplicationsUseCase)

    viewModelOf(::ClipboardViewModel)
}