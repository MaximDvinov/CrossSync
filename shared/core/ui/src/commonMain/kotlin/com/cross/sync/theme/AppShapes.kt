package com.cross.sync.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

class AppShapes {
    val round8: RoundedCornerShape
        get() = RoundedCornerShape(8.dp)
    val round10: RoundedCornerShape
        get() = RoundedCornerShape(10.dp)
    val round12: RoundedCornerShape
        get() = RoundedCornerShape(12.dp)
    val round16: RoundedCornerShape
        get() = RoundedCornerShape(16.dp)
    val round20: RoundedCornerShape
        get() = RoundedCornerShape(20.dp)
    val round24: RoundedCornerShape
        get() = RoundedCornerShape(24.dp)
    val round24Top: RoundedCornerShape
        get() = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val round32: RoundedCornerShape
        get() = RoundedCornerShape(32.dp)
    val round50percent: RoundedCornerShape
        get() = RoundedCornerShape(50)
}

val LocalAppShapes = staticCompositionLocalOf { AppShapes() }