package com.cross.sync.clipboard.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import com.cross.sync.theme.icons.CrossSync
import com.cross.sync.theme.icons.More

@Composable
fun TopBar(
    onClose: (() -> Unit)?,
    onOpenSettings: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppTheme.icons.CrossSync,
            contentDescription = null,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(30.dp)
        )

        BasicText(
            modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
            text = "CrossSync",
            style = AppTheme.typography.semiBold20.copy(
                color = AppTheme.colors.primary,
                textAlign = if (onClose == null && onOpenSettings == null) TextAlign.Start else TextAlign.Center,
            )
        )

        onOpenSettings?.let { openSettings ->
            Box(
                Modifier
                    .clickable(onClick = openSettings)
                    .size(30.dp)
                    .clip(AppTheme.shapes.round10)
                    .background(AppTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTheme.icons.More,
                    contentDescription = null,
                    tint = AppTheme.colors.onSurfaceVariant
                )
            }
        }

        onClose?.let {
            Box(
                Modifier
                    .clickable(onClick = it)
                    .size(30.dp)
                    .clip(AppTheme.shapes.round10)
                    .background(AppTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTheme.icons.Close,
                    contentDescription = null,
                    tint = AppTheme.colors.onSurfaceVariant
                )
            }
        }

    }
}
