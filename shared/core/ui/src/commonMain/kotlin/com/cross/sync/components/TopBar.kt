package com.cross.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    leftAction: @Composable (RowScope.() -> Unit)? = null,
    rightAction: @Composable (RowScope.() -> Unit)? = null,
    text: String,
    color: Color = AppTheme.colors.primary,
    titleAlign: TextAlign = if (leftAction == null || rightAction == null) TextAlign.Start else TextAlign.Center,
    titleStyle: TextStyle = AppTheme.typography.semiBold28.copy(
        textAlign = titleAlign,
        color = color
    ),
    space: Dp = 10.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leftAction != null) {
            leftAction()
        }

        BasicText(
            modifier = Modifier.weight(1f),
            maxLines = 1,
            text = text,
            style = titleStyle
        )

        if (rightAction != null) {
            rightAction()
        }
    }
}