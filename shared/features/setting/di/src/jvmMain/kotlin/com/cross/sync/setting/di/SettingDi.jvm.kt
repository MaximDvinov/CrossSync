package com.cross.sync.setting.di

import com.cross.sync.setting.presentation.DesktopSettingViewModel
import com.cross.sync.setting.presentation.SettingViewModel
import com.cross.sync.syncing.domain.usecases.AddDeviceUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val settingModule: Module = module {
    singleOf(::AddDeviceUseCase)

    viewModelOf(::DesktopSettingViewModel) bind SettingViewModel::class
}