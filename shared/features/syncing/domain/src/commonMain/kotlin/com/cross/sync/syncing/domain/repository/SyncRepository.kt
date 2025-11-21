package com.cross.sync.syncing.domain.repository

import com.cross.sync.syncing.domain.entity.ClipboardMessage

interface SyncRepository {
    suspend fun sendCopiedData(message: ClipboardMessage): Result<Unit>
    suspend fun startSyncWithDb()
}