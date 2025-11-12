@file:OptIn( ExperimentalTime::class)

package com.cross.sync.clipboard.data.mappers

import com.cross.sync.clipboard.db.entities.CopiedDataEntity
import com.cross.sync.clipboard.db.entities.CopiedDataType
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

fun CopiedData.toEntity(): CopiedDataEntity {
    return when (this) {
        is CopiedData.File ->
            CopiedDataEntity(
                id = id,
                type = CopiedDataType.FILE,
                dateTime = dateTime.toEpochMilliseconds(),
                filePaths = filePaths,
                applicationId = applicationId,
                content = filePaths.joinToString()
            )


        is CopiedData.FormattedText ->
            CopiedDataEntity(
                id = id,
                type = CopiedDataType.FORMATTED_TEXT,
                dateTime = dateTime.toEpochMilliseconds(),
                content = text,
                mimeType = mimeType,
                plainText = plainText,
                applicationId = applicationId
            )


        is CopiedData.Image -> CopiedDataEntity(
            id = id,
            type = CopiedDataType.IMAGE,
            dateTime = dateTime.toEpochMilliseconds(),
            content = imagePath,
            applicationId = applicationId
        )

        is CopiedData.Text -> CopiedDataEntity(
            id = id,
            type = CopiedDataType.TEXT,
            dateTime = dateTime.toEpochMilliseconds(),
            content = text,
            applicationId = applicationId
        )
    }
}

fun CopiedDataEntity.toDomain(): CopiedData {
    return when (type) {
        CopiedDataType.TEXT -> CopiedData.Text(
            id = id,
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId,
            text = content
        )

        CopiedDataType.IMAGE -> CopiedData.Image(
            id = id,
            imagePath = content,
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )

        CopiedDataType.FILE -> CopiedData.File(
            id = id,
            filePaths = filePaths ?: listOf(),
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )

        CopiedDataType.FORMATTED_TEXT -> CopiedData.FormattedText(
            id = id,
            text = content,
            mimeType = mimeType ?: "",
            plainText = plainText ?: "",
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )
    }
}