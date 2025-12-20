package com.cross.sync.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme

@Stable
class ButtonsColors internal constructor(
    internal val containerColor: Color,
    internal val contentColor: Color,
    internal val disabledContainerColor: Color,
    internal val disabledContentColor: Color,
)

@Stable
class ButtonsDefaults internal constructor(
    internal val colors: ButtonsColors,
    internal val shape: Shape,
    internal val contentPadding: PaddingValues,
) {
    companion object {
        internal fun padding(
            horizontal: Dp = 16.dp,
            vertical: Dp = 8.dp,
        ) = PaddingValues(horizontal = horizontal, vertical = vertical)

        @Composable
        fun buttonColors(
            containerColor: Color = AppTheme.colors.surfaceVariant,
            contentColor: Color = AppTheme.colors.onSurfaceVariant,
            disabledContainerColor: Color = AppTheme.colors.outline,
            disabledContentColor: Color = AppTheme.colors.onPrimary,
        ): ButtonsDefaults = ButtonsDefaults(
            colors = ButtonsColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor

            ),
            shape = AppTheme.shapes.round10,
            contentPadding = padding()
        )

        @Composable
        fun buttonPadding(
            horizontal: Dp = 16.dp,
            vertical: Dp = 8.dp,
            shape: Shape = AppTheme.shapes.round10,
            containerColor: Color = AppTheme.colors.surfaceVariant,
            contentColor: Color = AppTheme.colors.onSurfaceVariant,
            disabledContainerColor: Color = AppTheme.colors.outline,
            disabledContentColor: Color = AppTheme.colors.onPrimary,
        ): ButtonsDefaults = ButtonsDefaults(
            colors = ButtonsColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            ),
            shape = shape,
            contentPadding = padding(horizontal, vertical)
        )

        @Composable
        fun roundedButtonColors(
            shape: Shape = AppTheme.shapes.round10,
            padding: PaddingValues = padding(),
            containerColor: Color = AppTheme.colors.surfaceVariant,
            contentColor: Color = AppTheme.colors.onSurfaceVariant,
            disabledContainerColor: Color = AppTheme.colors.outline,
            disabledContentColor: Color = AppTheme.colors.onPrimary,
        ): ButtonsDefaults = ButtonsDefaults(
            colors = ButtonsColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            ),
            shape = shape,
            contentPadding = padding
        )

        @Composable
        fun iconButtonColors(
            shape: Shape = AppTheme.shapes.round10,
            padding: PaddingValues = padding(8.dp),
            containerColor: Color = AppTheme.colors.surfaceVariant,
            contentColor: Color = AppTheme.colors.onSurfaceVariant,
            disabledContainerColor: Color = AppTheme.colors.outline,
            disabledContentColor: Color = AppTheme.colors.onPrimary,
        ): ButtonsDefaults = ButtonsDefaults(
            colors = ButtonsColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            ),
            shape = shape,
            contentPadding = padding
        )
    }

    @Composable
    fun copy(
        colors: ButtonsColors = this.colors,
        shape: Shape = this.shape,
        contentPadding: PaddingValues = this.contentPadding,
    ) = ButtonsDefaults(colors, shape, contentPadding)
}