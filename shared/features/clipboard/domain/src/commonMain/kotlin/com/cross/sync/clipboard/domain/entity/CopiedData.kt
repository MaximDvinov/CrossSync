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