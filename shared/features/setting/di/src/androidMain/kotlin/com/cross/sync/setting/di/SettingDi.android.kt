package com.cross.sync.setting.di

import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.setting.presentation.AndroidSettingViewModel
import com.cross.sync.setting.presentation.SettingViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val settingModule: Module = module {
    singleOf(::SettingPreferencesStore)
    viewModelOf(::AndroidSettingViewModel) bind SettingViewModel::class
}
