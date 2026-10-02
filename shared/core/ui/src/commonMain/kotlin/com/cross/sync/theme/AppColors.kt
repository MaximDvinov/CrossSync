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
    val redContainer: Color = Color.Unspecified,
    val onRedContainer: Color = Color.Unspecified
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
        redContainer = Color(0xFFFFDCDC),
        onRedContainer = Color(0xFF875454)
    )
}

/**
 * A low-glare dark palette that preserves the app's blue connection accent.
 *
 * Surfaces are blue-grey rather than pure black so cards and controls retain
 * their hierarchy, while the lighter primary remains legible on dark screens.
 */
val darkColors by lazy {
    AppColors(
        primary = Color(0xFFA9C7FF),
        onPrimary = Color(0xFF00325C),
        surface = Color(0xFF111B25),
        onSurface = Color(0xFFE2EDF9),
        surfaceVariant = Color(0xFF243445),
        onSurfaceVariant = Color(0xFFB8D2F0),
        background = Color(0xFF0B141D),
        outline = Color(0xFF8FA2B7),
        redContainer = Color(0xFF5D2B32),
        onRedContainer = Color(0xFFFFDADD)
    )
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }
