package com.cross.sync.syncing.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal fun Context.hasLocalNetworkAccess(): Boolean {
    return Build.VERSION.SDK_INT < 37 ||
        checkSelfPermission(ACCESS_LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
}

internal class LocalNetworkPermissionRequiredException : SecurityException(
    "Разрешите доступ к локальной сети, чтобы синхронизироваться с Mac"
)

private const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
