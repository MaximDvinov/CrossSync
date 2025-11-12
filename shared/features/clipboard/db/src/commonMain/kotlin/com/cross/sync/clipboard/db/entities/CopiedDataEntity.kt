package com.cross.sync.clipboard.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/**
 * Represents a single record of a copied item in the database.
 * This version includes several best-practice improvements:
 * - Uses an Enum for type safety (`CopiedDataType`).
 * - Indexes the `dateTime` column for faster sorting queries.
 * - Uses a TypeConverter to natively handle the `List<String>` for file paths.
 */
@Entity(
    tableName = "copied_data",
    indices = [Index(value = ["dateTime"])]
)
data class CopiedDataEntity(
    @PrimaryKey
    val id: Int, // Corresponds to CopiedData.id (Uuid.toString())

    val type: CopiedDataType, // Enum for type safety.


    val dateTime: Long, // Corresponds to CopiedData.dateTime (Instant.toEpochMilliseconds())

    val applicationId: String?, // Corresponds to CopiedData.applicationId

    val content: String, // text, imagePath

    // Data fields for specific types
    val plainText: String? = null,
    val filePaths: List<String>? = null, // Stored as JSON string via TypeConverter
    val mimeType: String? = null
)

/**
 * Enum to provide type safety for the different kinds of copied data.
 * Room will store these as strings in the database (e.g., "TEXT", "IMAGE").
 */
enum class CopiedDataType {
    TEXT,
    IMAGE,
    FILE,
    FORMATTED_TEXT
}

/**
 * Type converters to allow Room to store complex types like List<String>.
 *
 * IMPORTANT: This class needs to be registered with your Room Database.
 * 1. Add this annotation to your @Database class:
 *    @TypeConverters(CopiedDataConverters::class)
 *
 * 2. Add the kotlinx.serialization dependency to this module's `build.gradle.kts`:
 *    implementation(libs.kotlinx.serialization.json)
 */
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
