package com.cross.sync.syncing.data.mappers

import com.cross.sync.clipboard.db.entities.CopiedDataEntity
import com.cross.sync.clipboard.db.entities.CopiedDataType
import com.cross.sync.syncing.domain.entity.ClipboardMessage
import com.cross.sync.syncing.network.ClipboardMessageDto

fun ClipboardMessage.toDto() = ClipboardMessageDto(
    id = id, content = content, date = date
)

fun ClipboardMessageDto.toDomain() = ClipboardMessage(
    id = id, content = content, date = date
)

fun ClipboardMessageDto.toEntity() = CopiedDataEntity(
    id = content.hashCode(), content = content, dateTime = date,
    type = CopiedDataType.TEXT,
    applicationId = "android",
)

fun CopiedDataEntity.toDto() = ClipboardMessageDto(
    id = id, content = content, date = dateTime
)