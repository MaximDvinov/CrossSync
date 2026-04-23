@file:OptIn(ExperimentalTime::class)

package com.cross.sync.syncing.network.model

import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
sealed class CopiedDataDto {
    abstract val id: Long
    abstract val dateTime: Instant
    abstract val applicationId: String?

    @Serializable
    @SerialName("Text")
    class TextDto(
        override val id: Long,
        val text: String,
        override val dateTime: Instant = Clock.System.now(),
        override val applicationId: String?,
    ) : CopiedDataDto() {
        override fun hashCode(): Int = text.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            return text == (other as? TextDto)?.text
        }
    }

    @Serializable
    @SerialName("Image")
    class ImageDto(
        override val id: Long,
        val imagePath: String,
        override val dateTime: Instant = Clock.System.now(),
        override val applicationId: String?,
    ) : CopiedDataDto() {
        override fun hashCode(): Int = imagePath.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            return imagePath == (other as? ImageDto)?.imagePath
        }
    }

    @Serializable
    @SerialName("File")
    class FileDto(
        override val id: Long,
        val filePaths: List<String>,
        override val dateTime: Instant = Clock.System.now(),
        override val applicationId: String?,
    ) : CopiedDataDto() {
        override fun hashCode(): Int = filePaths.fold(0) { acc, s -> acc * 31 + s.hashCode() }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? FileDto ?: return false
            return filePaths == o.filePaths
        }
    }

    @Serializable
    @SerialName("FormattedText")
    class FormattedTextDto(
        override val id: Long,
        val text: String,
        val mimeType: String,
        val plainText: String,
        override val dateTime: Instant = Clock.System.now(),
        override val applicationId: String?,
    ) : CopiedDataDto() {
        override fun hashCode(): Int = plainText.hashCode() * 31 + mimeType.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? FormattedTextDto ?: return false
            return plainText == o.plainText && mimeType == o.mimeType
        }
    }
}

fun CopiedDataDto.toDomain(): CopiedData {
    return when (this) {
        is CopiedDataDto.FileDto -> CopiedData.File(
            id = id,
            filePaths = filePaths,
            dateTime = dateTime,
            applicationId = applicationId
        )

        is CopiedDataDto.FormattedTextDto -> CopiedData.FormattedText(
            id = id,
            text = text,
            mimeType = mimeType,
            plainText = plainText,
            dateTime = dateTime,
            applicationId = applicationId

        )

        is CopiedDataDto.ImageDto -> CopiedData.Image(
            id = id, imagePath = imagePath, dateTime = dateTime, applicationId = applicationId

        )

        is CopiedDataDto.TextDto -> CopiedData.Text(
            id = id, text = text, dateTime = dateTime, applicationId = applicationId
        )
    }
}

fun CopiedData.toDto(): CopiedDataDto {
    return when (this) {
        is CopiedData.File -> CopiedDataDto.FileDto(
            id = id,
            filePaths = filePaths,
            dateTime = dateTime,
            applicationId = applicationId
        )

        is CopiedData.FormattedText -> CopiedDataDto.FormattedTextDto(
            id = id,
            text = text,
            mimeType = mimeType,
            plainText = plainText,
            dateTime = dateTime,
            applicationId = applicationId

        )

        is CopiedData.Image -> CopiedDataDto.ImageDto(
            id = id, imagePath = imagePath, dateTime = dateTime, applicationId = applicationId

        )

        is CopiedData.Text -> CopiedDataDto.TextDto(
            id = id, text = text, dateTime = dateTime, applicationId = applicationId
        )
    }
}