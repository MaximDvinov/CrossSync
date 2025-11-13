package com.cross.sync.clipboard.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(getAppDataDir("CrossSync"), "clipboard.db")
    println("${dbFile.absolutePath}")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}

fun getAppDataDir(appName: String): File {
    val os = System.getProperty("os.name").lowercase()

    val baseDir = when {
        os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support")
        os.contains("win") -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"))
        os.contains("nux") || os.contains("nix") -> File(
            System.getProperty("user.home"),
            ".local/share"
        )

        else -> File(System.getProperty("user.home"))
    }

    val appDir = File(baseDir, appName)
    if (!appDir.exists()) appDir.mkdirs()
    return appDir
}