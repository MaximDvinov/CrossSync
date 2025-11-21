@file:OptIn(ExperimentalTime::class)

package com.cross.sync.syncing.data.repository

import com.cross.sync.clipboard.db.ClipboardDao
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.SystemClipboardRepository
import com.cross.sync.syncing.data.mappers.toDto
import com.cross.sync.syncing.data.mappers.toEntity
import com.cross.sync.syncing.domain.entity.ClipboardMessage
import com.cross.sync.syncing.domain.repository.SyncRepository
import com.cross.sync.syncing.network.LocalServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class DesktopSyncRepositoryImpl(
    private val localServer: LocalServer,
    private val dao: ClipboardDao,
    private val systemClipboardRepository: SystemClipboardRepository
) : SyncRepository {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    override suspend fun sendCopiedData(message: ClipboardMessage): Result<Unit> {
        return localServer.broadcast(message = message.toDto())
    }

    override suspend fun startSyncWithDb() {
        coroutineScope.launch {
            localServer.start()
        }

        coroutineScope.launch {
            localServer.observeMessages().collect { message ->
                message?.let { data ->
                    systemClipboardRepository.setData(CopiedData.Text(
                        id = data.id,
                        text = data.content,
                        applicationId = "Android" // TODO("Заменить потом")
                    ))
                }


            }
        }

        coroutineScope.launch {
            dao.getAllCopiedDataFlow().collect { list ->
                if (list.isNotEmpty()) {
                    localServer.broadcast(list.first().toDto())
                }
            }
        }

    }
}