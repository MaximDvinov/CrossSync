package com.cross.sync

import android.app.Application
import com.cross.sync.client.appModule
import com.cross.sync.clipboard.data.ClipboardManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val androidModule = module {
    singleOf(::AndroidClipboardManager) bind ClipboardManager::class
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(level = Level.ERROR)
            androidContext(this@App)
            modules(androidModule, appModule)
        }
    }
}