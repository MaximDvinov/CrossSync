package com.cross.sync.syncing.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SyncPayloadDto {
    @Serializable
    @SerialName("CopiedData")
    data class CopiedDataPayload(
        val data: CopiedDataDto
    ) : SyncPayloadDto()

    @Serializable
    @SerialName("Category")
    data class CategoryPayload(
        val categoryId: Long,
        val name: String
    ) : SyncPayloadDto()

    @Serializable
    @SerialName("CategoryBinding")
    data class CategoryBindingPayload(
        val categoryId: Long,
        val copiedDataId: Long
    ) : SyncPayloadDto()
}
