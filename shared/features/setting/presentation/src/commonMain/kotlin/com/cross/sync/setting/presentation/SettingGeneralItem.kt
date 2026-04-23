package com.cross.sync.setting.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme

@Composable
internal fun SettingGeneralItem(
    title: String,
    description: String,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BasicText(
                text = title,
                style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
            )
            BasicText(
                text = description,
                style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline)
            )
        }

        when {
            trailingContent != null -> trailingContent()
            trailingText != null -> {
                Box(
                    modifier = if (onTrailingClick != null) {
                        Modifier.clickable(onClick = onTrailingClick)
                    } else {
                        Modifier
                    }
                ) {
                    BasicText(
                        text = trailingText,
                        style = AppTheme.typography.medium14.copy(color = AppTheme.colors.primary)
                    )
                }
            }
        }
    }
}
