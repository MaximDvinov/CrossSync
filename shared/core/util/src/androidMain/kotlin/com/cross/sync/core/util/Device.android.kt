package com.cross.sync.core.util

import android.os.Build

actual fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL

    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model.replaceFirstChar { it.uppercase() }
    } else {
        "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
    }
}