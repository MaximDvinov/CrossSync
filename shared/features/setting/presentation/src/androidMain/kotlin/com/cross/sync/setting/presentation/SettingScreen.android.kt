package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background)
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
