@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.cross.sync.clipboard.domain.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class CopiedData(
    open val id: Uuid,
    val date: Instant,
) {
    class Text(
        id: Uuid = Uuid.random(),
        val text: String,
        date: Instant = Clock.System.now(),
    ) : CopiedData(id, date) {
        override fun hashCode(): Int = text.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false

            return text == (other as? Text)?.text
        }
    }

    class Image(
        id: Uuid = Uuid.random(),
        val imagePath: String,
        date: Instant = Clock.System.now(),
    ) : CopiedData(id, date) {
        override fun hashCode(): Int = imagePath.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false

            return imagePath == (other as? Image)?.imagePath
        }
    }

    class File(
        id: Uuid = Uuid.random(),
        val filePaths: List<String>,
        date: Instant = Clock.System.now(),
    ) : CopiedData(id, date) {
        override fun hashCode(): Int = filePaths.fold(0) { acc, s -> acc * 31 + s.hashCode() }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false

            val o = other as? File ?: return false
            return filePaths == o.filePaths
        }
    }

    class FormattedText(
        id: Uuid = Uuid.random(),
        val text: String,
        val mimeType: String,
        date: Instant = Clock.System.now(),
    ) : CopiedData(id, date) {
        override fun hashCode(): Int = text.hashCode() * 31 + mimeType.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false
            val o = other as? FormattedText ?: return false
            return text == o.text && mimeType == o.mimeType
        }
    }
}