package com.cross.sync.clipboard.presentation.model

import androidx.compose.ui.graphics.ImageBitmap

expect fun String.base64ToImageBitmap(): ImageBitmap
