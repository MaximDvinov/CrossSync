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
    copiedDataId = content.hashCode().toLong(), content = content, dateTime = date,
    type = CopiedDataType.TEXT,
    applicationId = "android",
)

fun CopiedDataEntity.toDto() = ClipboardMessageDto(
    id = copiedDataId, content = content, date = dateTime
)