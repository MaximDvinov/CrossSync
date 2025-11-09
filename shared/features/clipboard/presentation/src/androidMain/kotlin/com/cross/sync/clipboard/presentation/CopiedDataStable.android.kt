package com.cross.sync.clipboard.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlin.io.encoding.Base64

actual fun String.base64ToImageBitmap(): ImageBitmap {
    val decodedBytes = Base64.decode(this)
    val imageBitmap = decodedBytes.decodeToImageBitmap()

    return imageBitmap
}