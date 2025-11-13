package com.cross.sync.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

class AppShadow {
    val default: Shadow = Shadow(
        offset = DpOffset(0.dp, 4.dp),
        radius = 8.dp,
        alpha = 0.05f
    )
}

val LocalAppShadows = staticCompositionLocalOf { AppShadow() }