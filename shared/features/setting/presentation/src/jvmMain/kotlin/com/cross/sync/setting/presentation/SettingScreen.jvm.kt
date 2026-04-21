package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close

@Composable
actual fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(color = AppTheme.colors.background)
    ) {
        TopBar(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            text = "CrossSync",
            rightAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.Close,
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

        CategorySetting(
            state = state,
            onDelete = { (viewModel as DesktopSettingViewModel).deleteCategory(categoryId = it.id) },
            onRename = { newName, category ->
                (viewModel as DesktopSettingViewModel).renameCategory(
                    categoryId = category.id,
                    name = newName
                )
            },
            onAddCategory = { (viewModel as DesktopSettingViewModel).addCategory(it) }
        )

        DeviceSetting(
            state = state,
            onDelete = {},
            onCancel = { (viewModel as DesktopSettingViewModel).qrCodeCancel() },
            onAddDevice = {
                (viewModel as DesktopSettingViewModel).pairingDevice()
            }
        )
    }
}
