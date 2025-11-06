package com.cross.sync.clipboard.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.theme.AppTheme

@Suppress("NonSkippableComposable")
@Composable
fun CopiedDataItem(
    modifier: Modifier = Modifier,
    copiedData: CopiedData,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    when (copiedData) {
        is CopiedData.Text -> TextCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick
        )
    }
}

@Suppress("NonSkippableComposable")
@Composable
fun TextCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedData.Text,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {

    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Box(
        modifier = selectableModifier
            .clip(AppTheme.shapes.round10)
            .clickable(onClick = onClick)
            .background(AppTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = copiedData.text,
            style = AppTheme.typography.regular16.copy(color = AppTheme.colors.onSurface),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}