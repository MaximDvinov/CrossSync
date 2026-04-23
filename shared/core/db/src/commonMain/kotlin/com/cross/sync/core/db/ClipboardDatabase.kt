package com.cross.sync.core.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cross.sync.core.db.entities.ApplicationEntity
import com.cross.sync.core.db.entities.CategoryCopiedDataCrossRef
import com.cross.sync.core.db.entities.CategoryEntity
import com.cross.sync.core.db.entities.CopiedDataConverters
import com.cross.sync.core.db.entities.CopiedDataEntity
import com.cross.sync.core.db.entities.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [CopiedDataEntity::class, ApplicationEntity::class, CategoryEntity::class, CategoryCopiedDataCrossRef::class, DeviceEntity::class],
    version = 2,
)
@TypeConverters(CopiedDataConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getClipboardDao(): ClipboardDao
    abstract fun getApplicationDao(): ApplicationDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun getDeviceDao(): DeviceDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}