package com.cross.sync.clipboard.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "clipboard.db")
    println("${dbFile.absolutePath}")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}