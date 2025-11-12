package com.cross.sync.clipboard.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Application(
    @PrimaryKey
    val id: String,
    val image: String,
    val name: String,
)