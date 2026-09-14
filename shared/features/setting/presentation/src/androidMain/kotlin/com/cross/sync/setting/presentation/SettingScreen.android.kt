package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.ArrowLeft

@Composable
actual fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val androidViewModel = viewModel as AndroidSettingViewModel
    val context = LocalContext.current
    val notificationAccessEnabled = Settings.Secure
        .getString(context.contentResolver, "enabled_notification_listeners")
        .orEmpty()
        .split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { it.packageName == context.packageName }
    val installedApplications = remember(context) {
        context.packageManager.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .map { application ->
                NotificationApplication(
                    packageName = application.packageName,
                    name = context.packageManager.getApplicationLabel(application).toString(),
                )
            }
            .sortedBy(NotificationApplication::name)
            .toList()
    }
    var showExcludedApplications by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBar(
            text = "CrossSync",
            titleAlign = androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            leftAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.ArrowLeft,
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

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
                    title = "Clipboard Data Auto-Clear Timeout",
                    description = "Defines how long copied data remains in the clipboard before being automatically removed.",
                    trailingText = "${state.clipboardAutoClearTimeoutDays} days",
                    onTrailingClick = androidViewModel::cycleClipboardAutoClearTimeout
                )
                SettingGeneralItem(
                    title = "Notification Sync",
                    description = "Sends Android notifications to your paired Mac.",
                    trailingText = if (state.notificationSyncEnabled) "On" else "Off",
                    onTrailingClick = androidViewModel::toggleNotificationSync,
                )
                SettingGeneralItem(
                    title = "Notification Content",
                    description = "Turn off to sync only the source app, without titles or message text.",
                    trailingText = if (state.notificationContentEnabled) "On" else "Private",
                    onTrailingClick = androidViewModel::toggleNotificationContent,
                )
                SettingGeneralItem(
                    title = "Notification Access",
                    description = "CrossSync needs Android notification access before syncing can start.",
                    trailingText = if (notificationAccessEnabled) "Granted" else "Grant",
                    onTrailingClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )
                SettingGeneralItem(
                    title = "Excluded Applications",
                    description = "Notifications from these Android apps never leave this device.",
                    trailingText = if (state.excludedApplicationIds.isEmpty()) "None" else state.excludedApplicationIds.size.toString(),
                    onTrailingClick = { showExcludedApplications = !showExcludedApplications },
                )
                if (showExcludedApplications) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                    ) {
                        items(installedApplications, key = NotificationApplication::packageName) { application ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = application.packageName in state.excludedApplicationIds,
                                    onCheckedChange = {
                                        androidViewModel.toggleExcludedApplication(application.packageName)
                                    },
                                )
                                BasicText(
                                    text = application.name,
                                    style = AppTheme.typography.regular12.copy(color = AppTheme.colors.onSurface),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicText(
                text = "Device Sync",
                modifier = Modifier.padding(horizontal = 10.dp),
                style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surface, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BasicText(
                    text = "Manage all settings related to syncing with external devices, including server configuration, adding new devices, and viewing currently connected ones.",
                    style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline),
                    modifier = Modifier.fillMaxWidth()
                )

                val connectedDevice = state.devices.firstOrNull()
                if (connectedDevice == null) {
                    RoundedTextButton(
                        onClick = onOpenConnection,
                        text = "Create Connection",
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonsDefaults.buttonPadding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                        textStyle = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppTheme.colors.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BasicText(
                            text = "Connected Mac",
                            style = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.outline)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BasicText(
                                text = state.connectedDesktopName.ifBlank { "Mac" },
                                style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant)
                            )
                            BasicText(
                                text = "IP: ${state.connectedDesktopIp.ifBlank { "Unknown" }}",
                                style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedTextButton(
                                onClick = androidViewModel::refreshConnection,
                                text = "Refresh Connection",
                                colors = ButtonsDefaults.buttonPadding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                                textStyle = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onSurfaceVariant)
                            )
                            RoundedTextButton(
                                onClick = { androidViewModel.deleteDevice(connectedDevice.id) },
                                text = "Delete Connection",
                                colors = ButtonsDefaults.buttonPadding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp,
                                    containerColor = AppTheme.colors.redContainer,
                                    contentColor = AppTheme.colors.onRedContainer
                                ),
                                textStyle = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onRedContainer)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationApplication(
    val packageName: String,
    val name: String,
)
