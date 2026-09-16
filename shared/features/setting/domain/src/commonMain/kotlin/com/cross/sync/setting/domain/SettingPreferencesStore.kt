package com.cross.sync.setting.domain

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

enum class DesktopNotificationDelivery(
    val label: String,
) {
    BOTH("System + popup"),
    SYSTEM("System"),
    POPUP("Popup"),
    OFF("Off"),
    ;

    val showsSystem: Boolean
        get() = this == BOTH || this == SYSTEM

    val showsPopup: Boolean
        get() = this == BOTH || this == POPUP

    companion object {
        fun fromStoredValue(value: String): DesktopNotificationDelivery =
            entries.firstOrNull { it.name == value } ?: BOTH
    }
}

data class GeneralSettingsPreferences(
    val launchAtSystemStartup: Boolean = false,
    val localization: String = "English",
    val themeMode: String = "SYSTEM",
    val clipboardAutoClearTimeoutDays: Int = 15,
    val quickAccessHistorySize: Int = 50,
    val notificationSyncEnabled: Boolean = true,
    val notificationContentEnabled: Boolean = true,
    val notificationHistoryRetentionDays: Int = 7,
    val desktopNotificationDelivery: DesktopNotificationDelivery = DesktopNotificationDelivery.BOTH,
)

class SettingPreferencesStore(
    private val settings: Settings
) {
    fun getGeneralSettings(): GeneralSettingsPreferences {
        return GeneralSettingsPreferences(
            launchAtSystemStartup = settings[KEY_LAUNCH_AT_SYSTEM_STARTUP, false],
            localization = settings[KEY_LOCALIZATION, DEFAULT_LOCALIZATION],
            themeMode = settings[KEY_THEME_MODE, DEFAULT_THEME_MODE],
            clipboardAutoClearTimeoutDays = settings[KEY_AUTO_CLEAR_DAYS, DEFAULT_AUTO_CLEAR_DAYS],
            quickAccessHistorySize = settings[KEY_QUICK_ACCESS_HISTORY_SIZE, DEFAULT_QUICK_ACCESS_HISTORY_SIZE],
            notificationSyncEnabled = settings[KEY_NOTIFICATION_SYNC_ENABLED, true],
            notificationContentEnabled = settings[KEY_NOTIFICATION_CONTENT_ENABLED, true],
            notificationHistoryRetentionDays = settings[KEY_NOTIFICATION_HISTORY_RETENTION_DAYS, DEFAULT_NOTIFICATION_HISTORY_RETENTION_DAYS],
            desktopNotificationDelivery = DesktopNotificationDelivery.fromStoredValue(
                settings[
                    KEY_DESKTOP_NOTIFICATION_DELIVERY,
                    if (settings[KEY_DESKTOP_NOTIFICATION_BANNERS_ENABLED, true]) {
                        DesktopNotificationDelivery.BOTH.name
                    } else {
                        DesktopNotificationDelivery.OFF.name
                    },
                ],
            ),
        )
    }

    fun setLaunchAtSystemStartup(enabled: Boolean) {
        settings[KEY_LAUNCH_AT_SYSTEM_STARTUP] = enabled
    }

    fun setLocalization(localization: String) {
        settings[KEY_LOCALIZATION] = localization
    }

    fun setThemeMode(themeMode: String) {
        settings[KEY_THEME_MODE] = themeMode
    }

    fun setClipboardAutoClearTimeoutDays(days: Int) {
        settings[KEY_AUTO_CLEAR_DAYS] = days
    }

    fun setQuickAccessHistorySize(size: Int) {
        settings[KEY_QUICK_ACCESS_HISTORY_SIZE] = size
    }

    fun setNotificationSyncEnabled(enabled: Boolean) {
        settings[KEY_NOTIFICATION_SYNC_ENABLED] = enabled
    }

    fun setNotificationContentEnabled(enabled: Boolean) {
        settings[KEY_NOTIFICATION_CONTENT_ENABLED] = enabled
    }

    fun setNotificationHistoryRetentionDays(days: Int) {
        settings[KEY_NOTIFICATION_HISTORY_RETENTION_DAYS] = days
    }

    fun setDesktopNotificationDelivery(delivery: DesktopNotificationDelivery) {
        settings[KEY_DESKTOP_NOTIFICATION_DELIVERY] = delivery.name
        // Keep the old flag in sync for installations upgraded from earlier builds.
        settings[KEY_DESKTOP_NOTIFICATION_BANNERS_ENABLED] = delivery != DesktopNotificationDelivery.OFF
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
        private const val KEY_THEME_MODE = "setting.themeMode"
        private const val KEY_AUTO_CLEAR_DAYS = "setting.clipboardAutoClearTimeoutDays"
        private const val KEY_QUICK_ACCESS_HISTORY_SIZE = "setting.quickAccessHistorySize"
        private const val KEY_NOTIFICATION_SYNC_ENABLED = "setting.notificationSyncEnabled"
        private const val KEY_NOTIFICATION_CONTENT_ENABLED = "setting.notificationContentEnabled"
        private const val KEY_NOTIFICATION_HISTORY_RETENTION_DAYS = "setting.notificationHistoryRetentionDays"
        private const val KEY_DESKTOP_NOTIFICATION_BANNERS_ENABLED = "setting.desktopNotificationBannersEnabled"
        private const val KEY_DESKTOP_NOTIFICATION_DELIVERY = "setting.desktopNotificationDelivery"
        private const val KEY_EXCLUDED_APPLICATION_IDS = "setting.excludedApplicationIds"

        private const val EXCLUDED_SEPARATOR = "|"

        const val DEFAULT_LOCALIZATION = "English"
        const val DEFAULT_THEME_MODE = "SYSTEM"
        const val DEFAULT_AUTO_CLEAR_DAYS = 15
        const val DEFAULT_QUICK_ACCESS_HISTORY_SIZE = 50
        const val DEFAULT_NOTIFICATION_HISTORY_RETENTION_DAYS = 7

        val LOCALIZATION_OPTIONS = listOf("English", "Русский")
        val AUTO_CLEAR_DAYS_OPTIONS = listOf(1, 3, 7, 15, 30)
        val QUICK_ACCESS_SIZE_OPTIONS = listOf(10, 20, 50, 100, 200)
        val NOTIFICATION_HISTORY_RETENTION_DAYS_OPTIONS = listOf(1, 3, 7, 15, 30)
        val DESKTOP_NOTIFICATION_DELIVERY_OPTIONS = DesktopNotificationDelivery.entries
    }
}
