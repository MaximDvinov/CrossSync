package com.cross.sync.setting.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.PairingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
data class SettingState(
    val categories: List<Category> = listOf(),
    val devices: List<DeviceData> = listOf(),
    val pairingState: PairingState = PairingState.Idle()
)

abstract class SettingViewModel() : ViewModel() {
    internal val _state = MutableStateFlow(SettingState())
    val state = _state.asStateFlow()
}







