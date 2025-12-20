package com.cross.sync.clipboard.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.presentation.model.CategoryStable
import com.cross.sync.components.dragAndDrop.DragAndDropState
import com.cross.sync.theme.AppTheme

@Composable
fun CategoryItem(
    modifier: Modifier = Modifier.Companion,
    category: CategoryStable,
    onSelect: () -> Unit,
    isSelect: Boolean,
) {
    Column(
        modifier = modifier.clickable(onClick = onSelect).height(30.dp).padding(4.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        BasicText(
            text = category.name,
            style = AppTheme.typography.semiBold16Center.copy(color = if (isSelect) AppTheme.colors.onSurface else AppTheme.colors.outline)
        )

        AnimatedVisibility(isSelect) {
            Box(
                modifier = Modifier.Companion.height(2.dp).width(16.dp)
                    .background(color = AppTheme.colors.primary)
            )
        }
    }
}