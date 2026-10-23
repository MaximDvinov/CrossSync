@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cross.sync.core.db

import androidx.room.Room
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import org.koin.core.module.Module
import org.koin.dsl.module

fun createNativeDatabase(directory: String): AppDatabase {
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)) {
        "Cannot create CrossSync application data directory"
    }
    return getRoomDatabase(Room.databaseBuilder<AppDatabase>(name = "$directory/clipboard.db"))
}

actual val databaseModule: Module
    get() = module {
        single<AppDatabase> {
            val directory = NSHomeDirectory() + "/Library/Application Support/CrossSyncNative"
            createNativeDatabase(directory)
        }
        single { get<AppDatabase>().getClipboardDao() }
        single { get<AppDatabase>().getApplicationDao() }
        single { get<AppDatabase>().getCategoryDao() }
        single { get<AppDatabase>().getDeviceDao() }
        single { get<AppDatabase>().getNotificationDao() }
    }
