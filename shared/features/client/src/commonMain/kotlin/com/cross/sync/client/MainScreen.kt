package com.cross.sync.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.setting.presentation.SettingScreen
import com.cross.sync.setting.presentation.SettingViewModel
import com.cross.sync.syncing.presentation.ConnectionScreen
import com.cross.sync.syncing.presentation.ConnectDeviceViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private data object ClipboardRoute : NavKey

@Serializable
private data object SettingsRoute : NavKey

@Serializable
private data object ConnectionRoute : NavKey

@Composable
fun MainScreen(modifier: Modifier = Modifier, isClient: Boolean) {
    val connectDeviceViewModel: ConnectDeviceViewModel = koinInject()
    val settingViewModel: SettingViewModel = koinInject()

    val connectState by connectDeviceViewModel.state.collectAsState()
    val backStack = remember { NavBackStack<NavKey>(ClipboardRoute) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
    ) { key ->
        when (key) {
            ClipboardRoute -> NavEntry(key) {
                ClipboardScreen(
                    modifier = Modifier,
                    onOpenFullApp = { backStack.add(SettingsRoute) },
                    onSendToMac = connectDeviceViewModel::sendCurrentClipboardData,
                    isSendingToMac = connectState.isSendingCopiedData,
                    sendToMacError = connectState.sendError
                )
            }

            SettingsRoute -> NavEntry(key) {
                SettingScreen(
                    modifier = Modifier,
                    viewModel = settingViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenConnection = { backStack.add(ConnectionRoute) }
                )
            }

            ConnectionRoute -> NavEntry(key) {
                ConnectionScreen(
                    modifier = Modifier,
                    connectDeviceViewModel = connectDeviceViewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            else -> NavEntry(key) {}
        }
    }
}
