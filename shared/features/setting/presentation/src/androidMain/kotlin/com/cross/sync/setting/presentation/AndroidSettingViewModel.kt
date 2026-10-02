package com.cross.sync.setting.presentation

import androidx.lifecycle.viewModelScope
import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.domain.entity.SyncSettingsKeys
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.domain.repository.SyncRepository
import com.cross.sync.syncing.domain.usecases.ConnectToServerUseCase
import com.cross.sync.theme.AppThemeMode
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AndroidSettingViewModel(
    private val settingPreferencesStore: SettingPreferencesStore,
    private val deviceRepository: DeviceRepository,
    private val syncRepository: SyncRepository,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val settings: Settings,
    private val notificationSnapshotPublisher: NotificationSnapshotPublisher,
) : SettingViewModel() {
    init {
        val generalSettings = settingPreferencesStore.getGeneralSettings()
        _state.update {
            it.copy(
                clipboardAutoClearTimeoutDays = generalSettings.clipboardAutoClearTimeoutDays,
                themeMode = AppThemeMode.fromStoredValue(generalSettings.themeMode),
                notificationSyncEnabled = generalSettings.notificationSyncEnabled,
                notificationContentEnabled = generalSettings.notificationContentEnabled,
                excludedApplicationIds = settingPreferencesStore.getExcludedApplicationIds().sorted(),
            )
        }

        viewModelScope.launch {
            deviceRepository.observeDevices().collect { devices ->
                _state.update {
                    it.copy(
                        devices = devices,
                        connectedDesktopName = settings[SyncSettingsKeys.CONNECTED_DESKTOP_NAME, ""],
                        connectedDesktopIp = settings[SyncSettingsKeys.CONNECTED_DESKTOP_IP, ""]
                    )
                }
            }
        }
    }

    fun cycleClipboardAutoClearTimeout() {
        val newValue = cycleOption(
            current = _state.value.clipboardAutoClearTimeoutDays,
            options = SettingPreferencesStore.AUTO_CLEAR_DAYS_OPTIONS
        )
        settingPreferencesStore.setClipboardAutoClearTimeoutDays(newValue)
        _state.update { it.copy(clipboardAutoClearTimeoutDays = newValue) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        settingPreferencesStore.setThemeMode(themeMode.name)
        _state.update { it.copy(themeMode = themeMode) }
    }

    fun toggleNotificationSync() {
        val enabled = !_state.value.notificationSyncEnabled
        settingPreferencesStore.setNotificationSyncEnabled(enabled)
        _state.update { it.copy(notificationSyncEnabled = enabled) }
        refreshNotifications()
    }

    fun toggleNotificationContent() {
        val enabled = !_state.value.notificationContentEnabled
        settingPreferencesStore.setNotificationContentEnabled(enabled)
        _state.update { it.copy(notificationContentEnabled = enabled) }
        refreshNotifications()
    }

    fun toggleExcludedApplication(applicationId: String) {
        val excludedApplicationIds = _state.value.excludedApplicationIds.toSet()
        val updatedIds = if (applicationId in excludedApplicationIds) {
            excludedApplicationIds - applicationId
        } else {
            excludedApplicationIds + applicationId
        }
        settingPreferencesStore.setExcludedApplicationIds(updatedIds)
        _state.update { it.copy(excludedApplicationIds = updatedIds.sorted()) }
        refreshNotifications()
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(deviceId)
            settings[SyncSettingsKeys.CONNECTED_DESKTOP_NAME] = ""
            settings[SyncSettingsKeys.CONNECTED_DESKTOP_IP] = ""
            settings[SyncSettingsKeys.HOST] = ""
            settings[SyncSettingsKeys.HOSTS] = ""
            settings[SyncSettingsKeys.PORT] = 0
            syncRepository.disconnect()
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            syncRepository.disconnect()
            connectToServerUseCase()
        }
    }

    private fun <T> cycleOption(current: T, options: List<T>): T {
        if (options.isEmpty()) return current
        val currentIndex = options.indexOf(current)
        return if (currentIndex == -1) options.first() else options[(currentIndex + 1) % options.size]
    }

    private fun refreshNotifications() {
        viewModelScope.launch {
            notificationSnapshotPublisher.publishActive()
        }
    }
}
