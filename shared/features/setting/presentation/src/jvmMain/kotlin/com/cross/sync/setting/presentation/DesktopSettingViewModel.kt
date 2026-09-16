package com.cross.sync.setting.presentation

import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.usecase.AddCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.GetApplicationsUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.RenameCategoryUseCase
import com.cross.sync.setting.domain.DesktopAutoStartManager
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.usecases.AddDeviceUseCase
import com.cross.sync.syncing.domain.usecases.DeleteDeviceUseCase
import com.cross.sync.syncing.domain.usecases.ObserveDevicesUseCase
import com.cross.sync.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DesktopSettingViewModel(
    private val observeCategoryUseCase: ObserveCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val renameCategoryUseCase: RenameCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val addDeviceUseCase: AddDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase,
    private val getApplicationsUseCase: GetApplicationsUseCase,
    private val settingPreferencesStore: SettingPreferencesStore,
    private val desktopAutoStartManager: DesktopAutoStartManager
) : SettingViewModel() {
    init {
        val generalSettings = settingPreferencesStore.getGeneralSettings()
        val launchAtSystemStartup = resolveLaunchAtSystemStartup(generalSettings.launchAtSystemStartup)

        _state.update {
            it.copy(
                launchAtSystemStartup = launchAtSystemStartup,
                localization = generalSettings.localization,
                themeMode = AppThemeMode.fromStoredValue(generalSettings.themeMode),
                clipboardAutoClearTimeoutDays = generalSettings.clipboardAutoClearTimeoutDays,
                quickAccessHistorySize = generalSettings.quickAccessHistorySize,
                notificationHistoryRetentionDays = generalSettings.notificationHistoryRetentionDays,
                desktopNotificationDelivery = generalSettings.desktopNotificationDelivery,
                excludedApplicationIds = settingPreferencesStore.getExcludedApplicationIds().sorted()
            )
        }

        viewModelScope.launch {
            observeCategoryUseCase().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }

        viewModelScope.launch {
            observeDevicesUseCase().collect { devices ->
                _state.update { it.copy(devices = devices) }
            }
        }

        viewModelScope.launch {
            getApplicationsUseCase().collect { applications ->
                _state.update { it.copy(applications = applications) }
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            addCategoryUseCase.invoke(category = Category(0, name))
        }
    }

    fun renameCategory(categoryId: Long, name: String) {
        viewModelScope.launch {
            renameCategoryUseCase(Category(categoryId, name))
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            deleteCategoryUseCase(categoryId)
        }
    }

    fun pairingDevice() {
        viewModelScope.launch {
            addDeviceUseCase().collect { state ->
                _state.update {
                    it.copy(pairingState = state)
                }
            }
        }
    }

    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            deleteDeviceUseCase(deviceId)
        }
    }

    fun qrCodeCancel() {
        _state.update {
            it.copy(pairingState = PairingState.Idle())
        }
    }

    fun toggleLaunchAtSystemStartup() {
        val targetValue = !_state.value.launchAtSystemStartup
        val appliedValue = if (desktopAutoStartManager.isSupported()) {
            val result = desktopAutoStartManager.setEnabled(targetValue)
            if (result.isSuccess) {
                desktopAutoStartManager.isEnabled()
            } else {
                _state.value.launchAtSystemStartup
            }
        } else {
            targetValue
        }

        settingPreferencesStore.setLaunchAtSystemStartup(appliedValue)
        _state.update { it.copy(launchAtSystemStartup = appliedValue) }
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

    fun cycleQuickAccessHistorySize() {
        val newValue = cycleOption(
            current = _state.value.quickAccessHistorySize,
            options = SettingPreferencesStore.QUICK_ACCESS_SIZE_OPTIONS
        )
        settingPreferencesStore.setQuickAccessHistorySize(newValue)
        _state.update { it.copy(quickAccessHistorySize = newValue) }
    }

    fun cycleNotificationHistoryRetention() {
        val newValue = cycleOption(
            current = _state.value.notificationHistoryRetentionDays,
            options = SettingPreferencesStore.NOTIFICATION_HISTORY_RETENTION_DAYS_OPTIONS,
        )
        settingPreferencesStore.setNotificationHistoryRetentionDays(newValue)
        _state.update { it.copy(notificationHistoryRetentionDays = newValue) }
    }

    fun cycleDesktopNotificationDelivery() {
        val delivery = cycleOption(
            current = _state.value.desktopNotificationDelivery,
            options = SettingPreferencesStore.DESKTOP_NOTIFICATION_DELIVERY_OPTIONS,
        )
        settingPreferencesStore.setDesktopNotificationDelivery(delivery)
        _state.update { it.copy(desktopNotificationDelivery = delivery) }
    }

    fun addExcludedApplication(applicationId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            settingPreferencesStore.addExcludedApplicationId(applicationId)
            _state.update {
                it.copy(excludedApplicationIds = settingPreferencesStore.getExcludedApplicationIds().sorted())
            }
        }
    }

    fun removeExcludedApplication(applicationId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            settingPreferencesStore.removeExcludedApplicationId(applicationId)
            _state.update {
                it.copy(excludedApplicationIds = settingPreferencesStore.getExcludedApplicationIds().sorted())
            }
        }
    }

    private fun <T> cycleOption(current: T, options: List<T>): T {
        if (options.isEmpty()) return current
        val currentIndex = options.indexOf(current)
        return if (currentIndex == -1) options.first() else options[(currentIndex + 1) % options.size]
    }

    private fun resolveLaunchAtSystemStartup(storedValue: Boolean): Boolean {
        val actualValue = if (desktopAutoStartManager.isSupported()) {
            desktopAutoStartManager.isEnabled()
        } else {
            storedValue
        }
        if (actualValue != storedValue) {
            settingPreferencesStore.setLaunchAtSystemStartup(actualValue)
        }
        return actualValue
    }
}
