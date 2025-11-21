@file:OptIn(ExperimentalTime::class)

package com.cross.sync.syncing.network

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class ClipboardMessageDto(
    val id: Int,
    val content: String,
    val date: Long = Clock.System.now().toEpochMilliseconds(),
)