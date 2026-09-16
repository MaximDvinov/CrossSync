package com.cross.sync.clipboard.presentation.ui

import Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import com.cross.sync.theme.icons.CrossSync
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell

@Composable
fun TopBar(
    onClose: (() -> Unit)?,
    onOpenSettings: (() -> Unit)? = null,
    onOpenHome: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    actionButtonSize: Dp = 32.dp,
    logoSize: Dp = 32.dp,
    contentPadding: Dp = 20.dp,
    topPadding: Dp = 10.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = contentPadding,
                end = contentPadding,
                top = topPadding,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onOpenHome != null) {
                    onOpenHome?.invoke()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = AppTheme.icons.CrossSync,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(logoSize)
            )

            BasicText(
                text = "CrossSync",
                style = AppTheme.typography.semiBold20.copy(
                    color = AppTheme.colors.primary,
                    textAlign = TextAlign.Start,
                )
            )
        }

        TopBarAction(
            onClick = onOpenNotifications,
            size = actionButtonSize
        ) {
            Icon(
                imageVector = FeatherIcons.Bell,
                contentDescription = "Notifications",
                tint = AppTheme.colors.onSurfaceVariant
            )
        }

        TopBarAction(
            onClick = onOpenSettings,
            size = actionButtonSize
        ) {
            Icon(
                imageVector = AppTheme.icons.Settings,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant
            )
        }

        TopBarAction(
            onClick = onClose,
            size = actionButtonSize
        ) {
            Icon(
                imageVector = AppTheme.icons.Close,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopBarAction(
    onClick: (() -> Unit)?,
    size: Dp,
    content: @Composable RowScope.() -> Unit
) {
    onClick?.let { action ->
        Row(
            modifier = Modifier
                .clickable(onClick = action)
                .size(size)
                .clip(AppTheme.shapes.round10)
                .background(AppTheme.colors.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}
