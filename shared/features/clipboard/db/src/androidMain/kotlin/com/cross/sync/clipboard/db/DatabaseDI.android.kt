package com.cross.sync.clipboard.db

import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module
    get() = module {
        single {
            getRoomDatabase(getDatabaseBuilder(get()))
        }

        single<ClipboardDao> {
            get<AppDatabase>().getClipboardDao()
        }

        single<ApplicationDao> {
            get<AppDatabase>().getApplicationDao()
        }
    }