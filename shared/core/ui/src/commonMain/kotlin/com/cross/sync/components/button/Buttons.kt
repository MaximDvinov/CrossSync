package com.cross.sync.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme

@Composable
fun RoundedIconButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonsDefaults = ButtonsDefaults.buttonPadding(8.dp),
    contentDescription: String? = null,
) {
    RoundedButton(
        onClick = onClick,
        modifier = modifier.height(IntrinsicSize.Min).width(intrinsicSize = IntrinsicSize.Min),
        enabled = enabled,
        colors = colors
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            tint = colors.colors.contentColor
        )
    }
}

@Composable
fun RoundedIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonsDefaults = ButtonsDefaults.buttonPadding(8.dp),
    contentDescription: String? = null,
) {
    RoundedButton(
        onClick = onClick,
        modifier = modifier.height(IntrinsicSize.Min).width(intrinsicSize = IntrinsicSize.Min),
        enabled = enabled,
        colors = colors
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            tint = colors.colors.contentColor
        )
    }
}

@Composable
fun RoundedIconButton(
    bitmap: ImageBitmap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonsDefaults = ButtonsDefaults.buttonPadding(8.dp),
    contentDescription: String? = null,
) {
    RoundedButton(
        onClick = onClick,
        modifier = modifier.height(IntrinsicSize.Min).width(intrinsicSize = IntrinsicSize.Min),
        enabled = enabled,
        colors = colors
    ) {
        Icon(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            tint = colors.colors.contentColor
        )
    }
}

@Composable
fun RoundedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    colors: ButtonsDefaults = ButtonsDefaults.buttonColors(),
    textStyle: TextStyle = AppTheme.typography.semiBold12Center.copy(color = colors.colors.contentColor),
) {
    RoundedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = {
            BasicText(
                modifier = Modifier,
                text = text,
                style = textStyle,
            )
        }
    )
}

@Composable
fun RoundedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonsDefaults = ButtonsDefaults.roundedButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    BaseButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun BaseButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: ButtonsDefaults,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick, enabled = enabled)
            .clip(colors.shape)
            .background(if (enabled) colors.colors.containerColor else colors.colors.disabledContainerColor)
            .padding(colors.contentPadding),
        content = content,
        horizontalArrangement = Arrangement.Center
    )
}
