package com.cross.sync.setting.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.setting.domain.DesktopNotificationDelivery
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.PairingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
data class SettingState(
    val launchAtSystemStartup: Boolean = false,
    val localization: String = "English",
    val clipboardAutoClearTimeoutDays: Int = 15,
    val quickAccessHistorySize: Int = 50,
    val notificationSyncEnabled: Boolean = true,
    val notificationContentEnabled: Boolean = true,
    val notificationHistoryRetentionDays: Int = 7,
    val desktopNotificationDelivery: DesktopNotificationDelivery = DesktopNotificationDelivery.BOTH,

    val applications: List<Application> = listOf(),
    val excludedApplicationIds: List<String> = listOf(),
    val connectedDesktopName: String = "",
    val connectedDesktopIp: String = "",

    val categories: List<Category> = listOf(),
    val devices: List<DeviceData> = listOf(),
    val pairingState: PairingState = PairingState.Idle()
)

abstract class SettingViewModel() : ViewModel() {
    internal val _state = MutableStateFlow(SettingState())
    val state = _state.asStateFlow()
}



