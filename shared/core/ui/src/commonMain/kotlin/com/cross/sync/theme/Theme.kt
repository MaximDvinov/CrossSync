package com.cross.sync.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.cross.sync.theme.icons.AppIcons
import com.cross.sync.theme.icons.LocalAppIcons
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import org.koin.compose.koinInject

enum class AppThemeMode(
    val label: String,
) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun next(): AppThemeMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStoredValue(value: String): AppThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

private const val THEME_MODE_KEY = "setting.themeMode"
private val themeModeState = mutableStateOf(AppThemeMode.SYSTEM)

@Composable
fun AppTheme(
    onThemeChanged: @Composable (isLight: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val settings = koinInject<Settings>()
    val systemIsDark = isSystemInDarkTheme()
    val scaleIndication = rememberScaleIndication()
    val themeMode = themeModeState.value
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemIsDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    LaunchedEffect(settings) {
        themeModeState.value = AppThemeMode.fromStoredValue(
            settings[THEME_MODE_KEY, AppThemeMode.SYSTEM.name]
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides if (isDark) darkColors else lightColors,
        LocalAppShapes provides AppShapes(),
        LocalAppIcons provides AppIcons(),
        LocalAppTypography provides AppTypography(),
        LocalRippleConfiguration provides RippleConfiguration(
            color = Color.Transparent,
            RippleAlpha(0f, 0f, 0f, 0f)
        ),
        LocalIndication provides scaleIndication,
    ) {
        onThemeChanged(!isDark)

        content()
    }
}

object AppTheme {
    fun updateThemeMode(mode: AppThemeMode) {
        themeModeState.value = mode
    }

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

    val shadows: AppShadow
        @Composable
        get() = AppShadow()
}


// TODO: Реализовать позже        LocalRippleConfiguration provides RippleConfiguration(
//            color = Color.Transparent,
//            RippleAlpha(0f, 0f, 0f, 0f)
//        ),
//        LocalIndication provides scaleIndication,
