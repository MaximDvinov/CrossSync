package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.ArrowLeft
import com.cross.sync.theme.icons.Close

@Composable
actual fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val desktopViewModel = viewModel as DesktopSettingViewModel

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background)
    ) {
        TopBar(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            text = "CrossSync",
            leftAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.ArrowLeft,
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                )
            },
            rightAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.Close,
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GeneralSettingDesktop(
                state = state,
                onToggleLaunchAtStartup = desktopViewModel::toggleLaunchAtSystemStartup,
                onCycleClipboardAutoClearTimeout = desktopViewModel::cycleClipboardAutoClearTimeout,
                onCycleQuickAccessHistorySize = desktopViewModel::cycleQuickAccessHistorySize,
                onCycleNotificationHistoryRetention = desktopViewModel::cycleNotificationHistoryRetention,
                onCycleDesktopNotificationDelivery = desktopViewModel::cycleDesktopNotificationDelivery,
            )

            CategorySetting(
                state = state,
                onDelete = { desktopViewModel.deleteCategory(categoryId = it.id) },
                onRename = { newName, category ->
                    desktopViewModel.renameCategory(
                        categoryId = category.id,
                        name = newName
                    )
                },
                onAddCategory = desktopViewModel::addCategory
            )

            ExcludedApplicationsSetting(
                state = state,
                onExcludeApp = desktopViewModel::addExcludedApplication,
                onRemoveExcludedApp = desktopViewModel::removeExcludedApplication
            )

            DeviceSetting(
                state = state,
                onDelete = { desktopViewModel.unpairDevice(it.id) },
                onCancel = desktopViewModel::qrCodeCancel,
                onAddDevice = desktopViewModel::pairingDevice
            )
        }
    }
}
