package com.cross.sync.setting.domain

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

data class GeneralSettingsPreferences(
    val launchAtSystemStartup: Boolean = false,
    val localization: String = "English",
    val clipboardAutoClearTimeoutDays: Int = 15,
    val quickAccessHistorySize: Int = 50,
)

class SettingPreferencesStore(
    private val settings: Settings
) {
    fun getGeneralSettings(): GeneralSettingsPreferences {
        return GeneralSettingsPreferences(
            launchAtSystemStartup = settings[KEY_LAUNCH_AT_SYSTEM_STARTUP, false],
            localization = settings[KEY_LOCALIZATION, DEFAULT_LOCALIZATION],
            clipboardAutoClearTimeoutDays = settings[KEY_AUTO_CLEAR_DAYS, DEFAULT_AUTO_CLEAR_DAYS],
            quickAccessHistorySize = settings[KEY_QUICK_ACCESS_HISTORY_SIZE, DEFAULT_QUICK_ACCESS_HISTORY_SIZE]
        )
    }

    fun setLaunchAtSystemStartup(enabled: Boolean) {
        settings[KEY_LAUNCH_AT_SYSTEM_STARTUP] = enabled
    }

    fun setLocalization(localization: String) {
        settings[KEY_LOCALIZATION] = localization
    }

    fun setClipboardAutoClearTimeoutDays(days: Int) {
        settings[KEY_AUTO_CLEAR_DAYS] = days
    }

    fun setQuickAccessHistorySize(size: Int) {
        settings[KEY_QUICK_ACCESS_HISTORY_SIZE] = size
    }

    fun getExcludedApplicationIds(): Set<String> {
        val raw = settings[KEY_EXCLUDED_APPLICATION_IDS, ""]
        if (raw.isBlank()) return emptySet()
        return raw.split(EXCLUDED_SEPARATOR).mapNotNull { value ->
            val trimmed = value.trim()
            if (trimmed.isBlank()) null else trimmed
        }.toSet()
    }

    fun setExcludedApplicationIds(ids: Set<String>) {
        settings[KEY_EXCLUDED_APPLICATION_IDS] = ids
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString(EXCLUDED_SEPARATOR)
    }

    fun addExcludedApplicationId(applicationId: String) {
        if (applicationId.isBlank()) return
        setExcludedApplicationIds(getExcludedApplicationIds() + applicationId)
    }

    fun removeExcludedApplicationId(applicationId: String) {
        if (applicationId.isBlank()) return
        setExcludedApplicationIds(getExcludedApplicationIds() - applicationId)
    }

    fun isApplicationExcluded(applicationId: String): Boolean {
        if (applicationId.isBlank()) return false
        return getExcludedApplicationIds().contains(applicationId)
    }

    companion object {
        private const val KEY_LAUNCH_AT_SYSTEM_STARTUP = "setting.launchAtSystemStartup"
        private const val KEY_LOCALIZATION = "setting.localization"
        private const val KEY_AUTO_CLEAR_DAYS = "setting.clipboardAutoClearTimeoutDays"
        private const val KEY_QUICK_ACCESS_HISTORY_SIZE = "setting.quickAccessHistorySize"
        private const val KEY_EXCLUDED_APPLICATION_IDS = "setting.excludedApplicationIds"

        private const val EXCLUDED_SEPARATOR = "|"

        const val DEFAULT_LOCALIZATION = "English"
        const val DEFAULT_AUTO_CLEAR_DAYS = 15
        const val DEFAULT_QUICK_ACCESS_HISTORY_SIZE = 50

        val LOCALIZATION_OPTIONS = listOf("English", "Русский")
        val AUTO_CLEAR_DAYS_OPTIONS = listOf(1, 3, 7, 15, 30)
        val QUICK_ACCESS_SIZE_OPTIONS = listOf(10, 20, 50, 100, 200)
    }
}
