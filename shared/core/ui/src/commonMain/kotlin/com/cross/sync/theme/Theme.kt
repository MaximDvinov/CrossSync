package com.cross.sync.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color


internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

@Composable
fun AppTheme(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(systemIsDark) }

    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState,
        LocalAppColors provides lightColors,
        LocalAppShapes provides AppShapes(),
        LocalAppIcons provides AppIcons(),
        LocalAppTypography provides AppTypography(),
    ) {
        val isDark by isDarkState
        onThemeChanged(!isDark)

        content()
    }
}

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val shapes: AppShapes
        @Composable
        get() = LocalAppShapes.current

    val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current

    val icons: AppIcons
        @Composable
        get() = LocalAppIcons.current
}









// TODO: Реализовать позже        LocalRippleConfiguration provides RippleConfiguration(
//            color = Color.Transparent,
//            RippleAlpha(0f, 0f, 0f, 0f)
//        ),
//        LocalIndication provides scaleIndication,