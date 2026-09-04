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

    /** A history item that should be persisted, but must not replace the system clipboard. */
    @Serializable
    @SerialName("HistoryCopiedData")
    data class HistoryCopiedDataPayload(
        val data: CopiedDataDto
    ) : SyncPayloadDto()

    /** The clipboard value currently selected on the desktop server. */
    @Serializable
    @SerialName("CurrentClipboard")
    data class CurrentClipboardPayload(
        val data: CopiedDataDto
    ) : SyncPayloadDto()

    /** IDs already stored by the client, used to send only missing history items. */
    @Serializable
    @SerialName("SnapshotRequest")
    data class SnapshotRequestPayload(
        val knownCopiedDataIds: Set<Long>
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
