@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.runtime.Stable
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@Stable
sealed class CopiedDataStable(
    open val id: Uuid,
    val date: Instant,
) {
    @Stable
    class Text(
        id: Uuid = Uuid.random(),
        val text: String,
        date: Instant = Clock.System.now(),
    ) : CopiedDataStable(id, date) {
        override fun hashCode(): Int {
            return text.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this::class != this::class) return false

            other as Text

            return text == other.text
        }
    }
}

fun CopiedData.toStable(): CopiedDataStable = when (this) {
    is CopiedData.Text -> CopiedDataStable.Text(id, text, date)
}

fun CopiedDataStable.toDomain(): CopiedData = when (this) {
    is CopiedDataStable.Text -> CopiedData.Text(id, text, date)
}