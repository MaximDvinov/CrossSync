package com.cross.sync.clipboard.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Delete
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cross.sync.clipboard.db.entities.ApplicationEntity
import com.cross.sync.clipboard.db.entities.CategoryCopiedDataCrossRef
import com.cross.sync.clipboard.db.entities.CategoryEntity
import com.cross.sync.clipboard.db.entities.CopiedDataConverters
import com.cross.sync.clipboard.db.entities.CopiedDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [CopiedDataEntity::class, ApplicationEntity::class, CategoryEntity::class, CategoryCopiedDataCrossRef::class],
    version = 1,
)
@TypeConverters(CopiedDataConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getClipboardDao(): ClipboardDao
    abstract fun getApplicationDao(): ApplicationDao
    abstract fun getCategoryDao(): CategoryDao
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