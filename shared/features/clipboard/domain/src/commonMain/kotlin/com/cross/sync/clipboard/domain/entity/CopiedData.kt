@file:OptIn(ExperimentalTime::class)

package com.cross.sync.clipboard.domain.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class CopiedData(
    val id: Int,
    val dateTime: Instant,
    val applicationId: String?,
) {
    class Text(
        id: Int,
        val text: String,
        dateTime: Instant = Clock.System.now(),
        applicationId: String?,
    ) : CopiedData(id, dateTime, applicationId) {
        override fun hashCode(): Int = text.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            return text == (other as? Text)?.text
        }
    }

    class Image(
        id: Int,
        val imagePath: String,
        dateTime: Instant = Clock.System.now(),
        applicationId: String?,
    ) : CopiedData(id, dateTime, applicationId) {
        override fun hashCode(): Int = imagePath.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            return imagePath == (other as? Image)?.imagePath
        }
    }

    class File(
        id: Int,
        val filePaths: List<String>,
        dateTime: Instant = Clock.System.now(),
        applicationId: String?,
    ) : CopiedData(id, dateTime, applicationId) {
        override fun hashCode(): Int = filePaths.fold(0) { acc, s -> acc * 31 + s.hashCode() }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? File ?: return false
            return filePaths == o.filePaths
        }
    }

    class FormattedText(
        id: Int,
        val text: String,
        val mimeType: String,
        val plainText: String,
        dateTime: Instant = Clock.System.now(),
        applicationId: String?,
    ) : CopiedData(id, dateTime, applicationId) {
        override fun hashCode(): Int = plainText.hashCode() * 31 + mimeType.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? FormattedText ?: return false
            return plainText == o.plainText && mimeType == o.mimeType
        }
    }
}