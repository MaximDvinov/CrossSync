package com.cross.sync.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val primary: Color = Color.Unspecified,
    val onPrimary: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val surfaceVariant: Color = Color.Unspecified,
    val onSurfaceVariant: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val outline: Color = Color.Unspecified,
)

val lightColors by lazy {
    AppColors(
        primary = Color(0xFF5F92DD),
        onPrimary = Color(0xFFF7FCFF),
        surface = Color(0xFFFEFFFF),
        onSurface = Color(0xFF00253C),
        surfaceVariant = Color(0xFFDCECFF),
        onSurfaceVariant = Color(0xFF02609B),
        background = Color(0xFFF0F9FF),
        outline = Color(0xFF7989A2),
    )
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }