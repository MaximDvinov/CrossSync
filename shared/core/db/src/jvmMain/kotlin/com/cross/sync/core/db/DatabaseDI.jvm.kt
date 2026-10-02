package com.cross.sync.core.db

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

        single<DeviceDao> {
            get<AppDatabase>().getDeviceDao()
        }

        single<NotificationDao> {
            get<AppDatabase>().getNotificationDao()
        }
    }
