package com.cross.sync.syncing.data.repository

import com.cross.sync.syncing.domain.entity.ClipboardMessage
import com.cross.sync.syncing.domain.repository.SyncRepository

class AndroidSyncRepositoryImpl(

): SyncRepository {
    override suspend fun sendCopiedData(message: ClipboardMessage): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun startSyncWithDb() {
        TODO("Not yet implemented")
    }
}