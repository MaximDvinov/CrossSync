package com.cross.sync.setting.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

@Composable
expect fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel = koinInject(),
    onBack: () -> Unit,
    onOpenConnection: () -> Unit
)
