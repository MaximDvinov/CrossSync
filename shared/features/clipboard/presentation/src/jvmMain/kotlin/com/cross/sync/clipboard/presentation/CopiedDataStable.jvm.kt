package com.cross.sync.clipboard.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64

actual fun String.base64ToImageBitmap(): ImageBitmap {
    val encodedImageData = Base64.decode(this)
    return Image.makeFromEncoded(encodedImageData).toComposeImageBitmap()
}