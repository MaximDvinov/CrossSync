package com.cross.sync.syncing.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.network.ClipboardClient
import com.cross.sync.syncing.network.runCatchingForApi
import kotlinx.coroutines.flow.Flow

class AndroidSyncRepositoryImpl(
    private val client: ClipboardClient,
    private val context: Context,
) : SyncRepositoryImpl(client) {
    override suspend fun connect(): Result<Flow<ClientConnectState>> {
        Log.i("AndroidSyncRepositoryImpl", " start connect")
        if (!context.hasLocalNetworkAccess()) {
            return Result.failure(LocalNetworkPermissionRequiredException())
        }
        val result = startServiceIfNeeded()
        return result.map { client.observeConnectedState() }
    }

    private fun startServiceIfNeeded(): Result<Unit> = runCatchingForApi {
        val intent = Intent(context, SyncForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun disconnect(): Result<Unit> = runCatchingForApi {
        super.disconnect().getOrThrow()
        context.stopService(Intent(context, SyncForegroundService::class.java))
    }
}
