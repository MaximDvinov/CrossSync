package com.cross.sync.core.util

actual fun getDeviceName(): String {
    val os = System.getProperty("os.name") ?: "Unknown OS"
    val host = runCatching {
        java.net.InetAddress.getLocalHost().hostName
    }.getOrNull()

    return if (host != null) {
        "$os ($host)"
    } else {
        os
    }
}