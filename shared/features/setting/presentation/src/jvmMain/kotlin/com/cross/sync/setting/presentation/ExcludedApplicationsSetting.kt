package com.cross.sync.setting.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import com.cross.sync.theme.icons.Plus
import com.cross.sync.theme.icons.Trash

@Composable
internal fun ExcludedApplicationsSetting(
    state: SettingState,
    onExcludeApp: (String) -> Unit,
    onRemoveExcludedApp: (String) -> Unit
) {
    var isPickerOpen by remember { mutableStateOf(false) }

    val excludedApps = state.applications.filter { app ->
        state.excludedApplicationIds.contains(app.id)
    }
    val availableApps = state.applications
        .filterNot { app -> state.excludedApplicationIds.contains(app.id) }
        .sortedBy { app -> app.name.lowercase() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "Excluded Applications",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.colors.surface)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicText(
                text = "List of apps from which clipboard data should not be captured.",
                style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline),
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                excludedApps.forEach { app ->
                    ExcludedApplicationChip(
                        app = app,
                        onDelete = { onRemoveExcludedApp(app.id) }
                    )
                }

                RoundedIconButton(
                    onClick = { isPickerOpen = true },
                    modifier = Modifier,
                    colors = ButtonsDefaults.buttonPadding(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                        shape = RoundedCornerShape(8.dp)
                    ),
                    imageVector = AppTheme.icons.Plus
                )
            }
        }
    }

    if (isPickerOpen) {
        Dialog(onDismissRequest = { isPickerOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Add Excluded App",
                        style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
                    )
                    RoundedIconButton(
                        onClick = { isPickerOpen = false },
                        imageVector = AppTheme.icons.Close,
                        colors = ButtonsDefaults.buttonPadding(
                            vertical = 6.dp,
                            horizontal = 6.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                    )
                }

                AnimatedVisibility(availableApps.isEmpty()) {
                    BasicText(
                        text = "No available apps to add.",
                        style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline)
                    )
                }

                if (availableApps.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableApps, key = { it.id }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTheme.colors.surfaceVariant)
                                    .clickable {
                                        onExcludeApp(app.id)
                                        isPickerOpen = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ApplicationIcon(app)
                                BasicText(
                                    text = app.name,
                                    style = AppTheme.typography.medium14.copy(color = AppTheme.colors.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcludedApplicationChip(
    app: Application,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ApplicationIcon(app)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(AppTheme.colors.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = app.name,
                style = AppTheme.typography.medium14.copy(AppTheme.colors.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }

        RoundedIconButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonsDefaults.buttonPadding(
                horizontal = 8.dp,
                vertical = 4.dp,
                shape = RoundedCornerShape(
                    bottomStart = 4.dp,
                    topStart = 4.dp,
                    bottomEnd = 8.dp,
                    topEnd = 8.dp
                ),
                containerColor = AppTheme.colors.redContainer,
                contentColor = AppTheme.colors.onRedContainer
            ),
            imageVector = AppTheme.icons.Trash
        )
    }
}

@Composable
private fun ApplicationIcon(app: Application) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 4.dp, bottomEnd = 4.dp))
            .background(AppTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val appIcon = app.icon
        if (appIcon.isNullOrBlank()) {
            BasicText(
                text = app.name.firstOrNull()?.uppercase() ?: "?",
                style = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onSurfaceVariant)
            )
        } else {
            AsyncImage(
                model = appIcon,
                contentDescription = app.name,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
