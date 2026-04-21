package com.cross.sync.core.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey
    val id: String,
    val icon: String?,
    val name: String,
    val path: String,
)