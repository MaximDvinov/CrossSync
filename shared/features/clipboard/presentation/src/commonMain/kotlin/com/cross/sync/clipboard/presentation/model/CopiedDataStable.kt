@file:OptIn(ExperimentalTime::class)

package com.cross.sync.clipboard.presentation.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@Stable
sealed class CopiedDataStable(
    open val id: Long,
    val date: LocalDateTime,
    val applicationId: String?,
    val application: Application? = null,
) {
    @Stable
    class Text(
        id: Long,
        val text: String,
        date: Instant = Clock.System.now(),
        applicationId: String?,
        application: Application? = null,
    ) : CopiedDataStable(
        id,
        date.toLocalDateTime(TimeZone.currentSystemDefault()),
        applicationId,
        application
    ) {
        override fun hashCode(): Int {
            return text.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this::class != this::class) return false
            if (id != (other as? Text)?.id) return false
            return text == other.text
        }
    }

    @Stable
    class FormattedText(
        id: Long,
        val text: String,
        val plainText: String,
        val mimeType: String, // "text/html" или "text/rtf"
        date: Instant = Clock.System.now(),
        applicationId: String?,
        application: Application? = null,
    ) : CopiedDataStable(
        id,
        date.toLocalDateTime(TimeZone.currentSystemDefault()),
        applicationId,
        application
    ) {
        override fun hashCode(): Int = text.hashCode() * 31 + mimeType.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? FormattedText ?: return false
            if (id != o.id) return false
            return text == o.text && mimeType == o.mimeType
        }
    }

    @Stable
    class Image(
        id: Long,
        val imagePath: String,
        date: Instant = Clock.System.now(),
        applicationId: String?,
        application: Application? = null,
    ) : CopiedDataStable(
        id,
        date.toLocalDateTime(TimeZone.currentSystemDefault()),
        applicationId,
        application
    ) {
        override fun hashCode(): Int = imagePath.hashCode()
        override fun equals(other: Any?): Boolean {
            println("$imagePath == ${(other as? Image)?.imagePath}")
            if (other == null || other::class != this::class) return false
            if (id != (other as? Image)?.id) return false
            return imagePath == other.imagePath
        }
    }

    @Stable
    class File(
        id: Long,
        val filePaths: List<String>,
        date: Instant = Clock.System.now(),
        applicationId: String?,
        application: Application? = null,
    ) : CopiedDataStable(
        id,
        date.toLocalDateTime(TimeZone.currentSystemDefault()),
        applicationId,
        application
    ) {
        override fun hashCode(): Int = filePaths.fold(0) { acc, s -> acc * 31 + s.hashCode() }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? File ?: return false
            if (id != o.id) return false
            return filePaths == o.filePaths
        }
    }
}

fun CopiedData.toStable(): CopiedDataStable = when (this) {
    is CopiedData.Text -> CopiedDataStable.Text(id, text, dateTime, applicationId)
    is CopiedData.FormattedText -> CopiedDataStable.FormattedText(
        id = id,
        text = text,
        plainText = plainText,
        mimeType = mimeType,
        date = dateTime,
        applicationId = applicationId,
    )

    is CopiedData.Image -> CopiedDataStable.Image(id, imagePath, dateTime, applicationId)
    is CopiedData.File -> CopiedDataStable.File(id, filePaths, dateTime, applicationId)
}

fun CopiedData.toStable(application: Application? = null): CopiedDataStable = when (this) {
    is CopiedData.Text -> CopiedDataStable.Text(id, text, dateTime, applicationId, application)
    is CopiedData.FormattedText -> CopiedDataStable.FormattedText(
        id = id,
        text = text,
        plainText = plainText,
        mimeType = mimeType,
        date = dateTime,
        applicationId = applicationId,
        application = application
    )

    is CopiedData.Image -> CopiedDataStable.Image(
        id,
        imagePath,
        dateTime,
        applicationId,
        application
    )

    is CopiedData.File -> CopiedDataStable.File(id, filePaths, dateTime, applicationId, application)
}

fun CopiedDataStable.toDomain(): CopiedData = when (this) {
    is CopiedDataStable.Text -> CopiedData.Text(
        id = id,
        text = text,
        dateTime = date.toInstant(TimeZone.currentSystemDefault()),
        applicationId = applicationId
    )

    is CopiedDataStable.FormattedText -> CopiedData.FormattedText(
        id = id,
        text = text,
        plainText = plainText,
        mimeType = mimeType,
        dateTime = date.toInstant(TimeZone.currentSystemDefault()),
        applicationId = applicationId
    )

    is CopiedDataStable.Image -> CopiedData.Image(
        id = id,
        imagePath = imagePath,
        dateTime = date.toInstant(TimeZone.currentSystemDefault()),
        applicationId = applicationId
    )

    is CopiedDataStable.File -> CopiedData.File(
        id = id,
        filePaths = filePaths,
        dateTime = date.toInstant(TimeZone.currentSystemDefault()),
        applicationId = applicationId
    )
}

expect fun String.base64ToImageBitmap(): ImageBitmap
