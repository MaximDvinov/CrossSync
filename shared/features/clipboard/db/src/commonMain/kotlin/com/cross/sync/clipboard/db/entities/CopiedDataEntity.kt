package com.cross.sync.clipboard.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json


@Entity(
    tableName = "copied_data",
    indices = [Index(value = ["dateTime"])]
)
data class CopiedDataEntity(
    @PrimaryKey
    val copiedDataId: Long,
    val type: CopiedDataType,
    val dateTime: Long,
    val applicationId: String?,
    val content: String,
    val plainText: String? = null,
    val filePaths: List<String>? = null,
    val mimeType: String? = null
)

enum class CopiedDataType {
    TEXT,
    IMAGE,
    FILE,
    FORMATTED_TEXT
}

class CopiedDataConverters {
    @TypeConverter
    fun fromStringList(filePaths: List<String>?): String? {
        return filePaths?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(filePathsJson: String?): List<String>? {
        return filePathsJson?.let { Json.decodeFromString(it) }
    }
}
