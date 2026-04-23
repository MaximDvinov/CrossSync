package com.cross.sync.client

import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.setting.di.settingModule
import com.cross.sync.syncing.di.syncingModule
import org.koin.dsl.module

val appModule = module {
    includes(clipboardModule, syncingModule, settingModule)
}