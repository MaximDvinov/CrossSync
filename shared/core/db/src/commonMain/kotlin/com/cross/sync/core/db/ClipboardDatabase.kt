package com.cross.sync.core.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cross.sync.core.db.entities.ApplicationEntity
import com.cross.sync.core.db.entities.CategoryCopiedDataCrossRef
import com.cross.sync.core.db.entities.CategoryEntity
import com.cross.sync.core.db.entities.CopiedDataConverters
import com.cross.sync.core.db.entities.CopiedDataEntity
import com.cross.sync.core.db.entities.DeviceEntity
import com.cross.sync.core.db.entities.SyncedNotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        CopiedDataEntity::class,
        ApplicationEntity::class,
        CategoryEntity::class,
        CategoryCopiedDataCrossRef::class,
        DeviceEntity::class,
        SyncedNotificationEntity::class,
    ],
    version = 5,
)
@TypeConverters(CopiedDataConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getClipboardDao(): ClipboardDao
    abstract fun getApplicationDao(): ApplicationDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun getDeviceDao(): DeviceDao
    abstract fun getNotificationDao(): NotificationDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
        connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS `synced_notifications` (
                `deviceId` TEXT NOT NULL,
                `notificationKey` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `appName` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `postedAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isRead` INTEGER NOT NULL,
                `isOngoing` INTEGER NOT NULL,
                `actionsJson` TEXT NOT NULL,
                PRIMARY KEY(`deviceId`, `notificationKey`)
            )
            """.trimIndent()
        ).use { it.step() }
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
        connection.prepare(
            "ALTER TABLE `synced_notifications` ADD COLUMN `groupKey` TEXT NOT NULL DEFAULT ''"
        ).use { it.step() }
        connection.prepare(
            "ALTER TABLE `synced_notifications` ADD COLUMN `isGroupSummary` INTEGER NOT NULL DEFAULT 0"
        ).use { it.step() }
        connection.prepare(
            "ALTER TABLE `synced_notifications` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'Other'"
        ).use { it.step() }
        connection.prepare(
            "ALTER TABLE `synced_notifications` ADD COLUMN `hasPreview` INTEGER NOT NULL DEFAULT 1"
        ).use { it.step() }
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
        connection.prepare(
            "ALTER TABLE `synced_notifications` ADD COLUMN `mediaJson` TEXT NOT NULL DEFAULT ''"
        ).use { it.step() }
    }
}
