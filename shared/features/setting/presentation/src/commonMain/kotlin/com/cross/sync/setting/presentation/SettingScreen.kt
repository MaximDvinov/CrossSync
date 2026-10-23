package com.cross.sync.setting.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cross.sync.components.rememberScreenViewModel

@Composable
expect fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel = rememberScreenViewModel(),
    onBack: () -> Unit,
    onOpenConnection: () -> Unit
)
