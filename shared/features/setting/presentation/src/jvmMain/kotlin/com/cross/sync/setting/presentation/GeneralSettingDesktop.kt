package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme

@Composable
internal fun GeneralSettingDesktop(
    state: SettingState,
    onToggleLaunchAtStartup: () -> Unit,
    onCycleClipboardAutoClearTimeout: () -> Unit,
    onCycleQuickAccessHistorySize: () -> Unit,
    onCycleNotificationHistoryRetention: () -> Unit,
    onCycleDesktopNotificationDelivery: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "General",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.surface, RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingGeneralItem(
                title = "Launch at System Startup",
                description = "Automatically starts the application when your computer boots."
            ) {
                StartupCheckbox(
                    checked = state.launchAtSystemStartup,
                    onToggle = onToggleLaunchAtStartup
                )
            }

            SettingGeneralItem(
                title = "Clipboard Data Auto-Clear Timeout",
                description = "Defines how long copied data remains in the clipboard before being automatically removed.",
                trailingText = "${state.clipboardAutoClearTimeoutDays} days",
                onTrailingClick = onCycleClipboardAutoClearTimeout
            )

            SettingGeneralItem(
                title = "Quick Access History Size",
                description = "Specifies how many recent copied items are shown in the Quick Access panel.",
                trailingText = state.quickAccessHistorySize.toString(),
                onTrailingClick = onCycleQuickAccessHistorySize
            )

            SettingGeneralItem(
                title = "Notification History",
                description = "Inactive notifications are kept for the selected number of days.",
                trailingText = "${state.notificationHistoryRetentionDays} days",
                onTrailingClick = onCycleNotificationHistoryRetention,
            )

            SettingGeneralItem(
                title = "macOS Notification Delivery",
                description = "Choose system notifications, the CrossSync popup, or both.",
                trailingText = state.desktopNotificationDelivery.label,
                onTrailingClick = onCycleDesktopNotificationDelivery,
            )
        }
    }
}

@Composable
private fun StartupCheckbox(
    checked: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.dp,
                color = if (checked) AppTheme.colors.primary.copy(alpha = 0.8f) else AppTheme.colors.outline.copy(
                    alpha = 0.8f
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = if (checked) AppTheme.colors.primary.copy(alpha = 0.8f) else AppTheme.colors.surface,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            BasicText(
                text = "✓",
                style = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onPrimary)
            )
        }
    }
}
