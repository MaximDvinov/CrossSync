package com.cross.sync.clipboard.db

import com.cross.sync.clipboard.db.entities.ApplicationEntity
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module
    get() = module {
        single<AppDatabase> {
            getRoomDatabase(getDatabaseBuilder())
        }

        single<ClipboardDao> {
            get<AppDatabase>().getClipboardDao()
        }

        single<ApplicationDao> {
            get<AppDatabase>().getApplicationDao()
        }

        single<CategoryDao> {
            get<AppDatabase>().getCategoryDao()
        }
    }