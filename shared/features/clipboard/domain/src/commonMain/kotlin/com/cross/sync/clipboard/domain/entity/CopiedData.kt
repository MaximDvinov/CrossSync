package com.cross.sync.clipboard.domain.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class CopiedData @OptIn(ExperimentalUuidApi::class) constructor(
    open val id: Uuid,
) {
    class Text @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class) constructor(
        id: Uuid = Uuid.random(),
        val text: String,
        val date: Instant,
    ) : CopiedData(id)
}