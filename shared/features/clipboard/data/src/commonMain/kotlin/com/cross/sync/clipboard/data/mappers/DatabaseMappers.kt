@file:OptIn(ExperimentalTime::class)

package com.cross.sync.clipboard.data.mappers

import com.cross.sync.core.db.entities.ApplicationEntity
import com.cross.sync.core.db.entities.CategoryEntity
import com.cross.sync.core.db.entities.CategoryWithCopiedData
import com.cross.sync.core.db.entities.CopiedDataEntity
import com.cross.sync.core.db.entities.CopiedDataType
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun CopiedData.toEntity(): CopiedDataEntity {
    return when (this) {
        is CopiedData.File ->
            CopiedDataEntity(
                copiedDataId = id,
                type = CopiedDataType.FILE,
                dateTime = dateTime.toEpochMilliseconds(),
                filePaths = filePaths,
                applicationId = applicationId,
                content = filePaths.joinToString()
            )


        is CopiedData.FormattedText ->
            CopiedDataEntity(
                copiedDataId = id,
                type = CopiedDataType.FORMATTED_TEXT,
                dateTime = dateTime.toEpochMilliseconds(),
                content = text,
                mimeType = mimeType,
                plainText = plainText,
                applicationId = applicationId
            )


        is CopiedData.Image -> CopiedDataEntity(
            copiedDataId = id,
            type = CopiedDataType.IMAGE,
            dateTime = dateTime.toEpochMilliseconds(),
            content = imagePath,
            applicationId = applicationId
        )

        is CopiedData.Text -> CopiedDataEntity(
            copiedDataId = id,
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
            id = copiedDataId,
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId,
            text = content
        )

        CopiedDataType.IMAGE -> CopiedData.Image(
            id = copiedDataId,
            imagePath = content,
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )

        CopiedDataType.FILE -> CopiedData.File(
            id = copiedDataId,
            filePaths = filePaths ?: listOf(),
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )

        CopiedDataType.FORMATTED_TEXT -> CopiedData.FormattedText(
            id = copiedDataId,
            text = content,
            mimeType = mimeType ?: "",
            plainText = plainText ?: "",
            dateTime = Instant.fromEpochMilliseconds(dateTime),
            applicationId = applicationId
        )
    }
}

fun ApplicationEntity.toDomain() = Application(
    id = id, name = name, icon = icon, path = path
)

fun Application.toEntity() = ApplicationEntity(
    id = id, name = name, icon = icon, path = path
)

fun CategoryWithCopiedData.toDomain() = Category(
    id = category.categoryId,
    name = category.name,
)

fun Category.toEntity() = CategoryEntity(
    categoryId = id, name = name
)

fun CategoryEntity.toDomain() = Category(
    id = categoryId, name = name
)