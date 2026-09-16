import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.usecase.ClearCopiedDataOlderThanUseCase
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.usecase.SaveApplicationsUseCase
import com.cross.sync.setting.di.settingModule
import com.cross.sync.notifications.di.desktopNotificationModule
import com.cross.sync.notifications.domain.usecase.ObserveNotificationsUseCase
import com.cross.sync.notifications.domain.usecase.ClearNotificationHistoryUseCase
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.di.syncingModule
import com.cross.sync.syncing.domain.usecases.ObservePairingUseCase
import com.cross.sync.syncing.domain.usecases.StartSyncUseCase
import com.tulskiy.keymaster.common.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.math.max
import kotlin.time.Clock
import utils.getInstalledApplications
import windows.GeneralWindow
import windows.GeneralWindowState
import windows.HomeTab
import windows.QuickClipboardWindow
import windows.MacOsLiveUpdateChip
import kotlin.time.ExperimentalTime
import notifications.NotificationBannerCoordinator
import notifications.MacOsLiveUpdateStatusItem
import windows.MacOsNotificationPopup

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyManager)
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication({
        modules(
            desktopModule,
            syncingModule,
            clipboardModule,
            settingModule,
            desktopNotificationModule,
        )
    }) {
        val startSyncUseCase = koinInject<StartSyncUseCase>()
        val observePairingUseCase = koinInject<ObservePairingUseCase>()
        val saveApplicationsUseCase = koinInject<SaveApplicationsUseCase>()
        val clearCopiedDataOlderThanUseCase = koinInject<ClearCopiedDataOlderThanUseCase>()
        val clearNotificationHistoryUseCase = koinInject<ClearNotificationHistoryUseCase>()
        val settingPreferencesStore = koinInject<SettingPreferencesStore>()
        val globalHotkeyManager = koinInject<GlobalHotkeyManager>()
        val observeNotificationsUseCase = koinInject<ObserveNotificationsUseCase>()
        val notificationsFlow = remember(observeNotificationsUseCase) {
            observeNotificationsUseCase()
        }
        val notifications by notificationsFlow.collectAsState(initial = emptyList())
        var generalWindowShowed by remember { mutableStateOf<GeneralWindowState?>(null) }
        var selectedHomeTab by remember { mutableStateOf(HomeTab.CLIPBOARD) }
        var popupNotifications by remember { mutableStateOf<List<SyncedNotification>>(emptyList()) }
        val notificationBannerCoordinator = remember {
            NotificationBannerCoordinator(
                showPopup = { notification ->
                    popupNotifications = popupNotifications
                        .filterNot { current ->
                            current.deviceId == notification.deviceId &&
                                current.notificationKey == notification.notificationKey
                        }
                        .plus(notification)
                },
                hidePopup = { id ->
                    popupNotifications = popupNotifications.filterNot {
                        "${it.deviceId}:${it.notificationKey}" == id
                    }
                },
                notificationDelivery = {
                    settingPreferencesStore.getGeneralSettings().desktopNotificationDelivery
                },
            )
        }

        val pairingState by observePairingUseCase().collectAsState()
        var showLiveUpdateChip by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            launch {
                val applications = getInstalledApplications() + Application(
                    id = "com.apple.finder",
                    name = "Finder",
                    null,
                    ""
                )
                saveApplicationsUseCase(applications)
            }
        }

        LaunchedEffect(Unit) {
            startSyncUseCase().collect {  }
        }

        LaunchedEffect(notifications, notificationBannerCoordinator) {
            notificationBannerCoordinator.onNotificationsChanged(notifications)
        }

        val liveUpdateNotification = notifications
            .asSequence()
            .filter { it.isActive && it.isOngoing }
            .maxByOrNull { it.updatedAt }

        LaunchedEffect(liveUpdateNotification?.deviceId, liveUpdateNotification?.notificationKey) {
            if (liveUpdateNotification != null) {
                showLiveUpdateChip = true
                delay(LIVE_UPDATE_POPUP_DURATION_MILLIS)
                showLiveUpdateChip = false
            }
        }

        LaunchedEffect(Unit) {
            launch(Dispatchers.Default) {
                while (true) {
                    val days = max(
                        settingPreferencesStore.getGeneralSettings().clipboardAutoClearTimeoutDays,
                        1
                    )
                    val olderThanEpochMillis =
                        Clock.System.now().toEpochMilliseconds() - days * MILLIS_IN_DAY
                    val notificationHistoryDays = max(
                        settingPreferencesStore.getGeneralSettings().notificationHistoryRetentionDays,
                        1,
                    )
                    val notificationHistoryCutoff =
                        Clock.System.now().toEpochMilliseconds() - notificationHistoryDays * MILLIS_IN_DAY

                    try {
                        clearCopiedDataOlderThanUseCase(olderThanEpochMillis)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // A failed cleanup must not stop syncing.
                    }
                    try {
                        clearNotificationHistoryUseCase.before(notificationHistoryCutoff)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // A failed cleanup must not stop syncing.
                    }

                    delay(AUTO_CLEAR_CHECK_INTERVAL_MILLIS)
                }
            }
        }

        QuickClipboardWindow(
            openSetting = {
                generalWindowShowed = GeneralWindowState.SETTING
            },
            openHome = {
                selectedHomeTab = HomeTab.CLIPBOARD
                generalWindowShowed = GeneralWindowState.GENERAL
            },
            globalHotkeyManager = globalHotkeyManager,
            pairingState = pairingState,
        )

        MacOsLiveUpdateStatusItem(
            notification = liveUpdateNotification,
            onClick = { showLiveUpdateChip = true },
        )

        MacOsLiveUpdateChip(
            notification = liveUpdateNotification,
            visible = showLiveUpdateChip,
            onDismiss = { showLiveUpdateChip = false },
        )

        MacOsNotificationPopup(
            notifications = popupNotifications,
            onDismiss = { notification ->
                popupNotifications = popupNotifications.filterNot {
                    it.deviceId == notification.deviceId &&
                        it.notificationKey == notification.notificationKey &&
                        it.updatedAt == notification.updatedAt
                }
            },
            onDismissAll = { popupNotifications = emptyList() },
        )

        generalWindowShowed?.let {
            GeneralWindow(
                generalWindowState = it,
                initialHomeTab = selectedHomeTab,
            ) {
                generalWindowShowed = null
            }
        }
    }
}

private const val MILLIS_IN_DAY = 24L * 60L * 60L * 1000L
private const val AUTO_CLEAR_CHECK_INTERVAL_MILLIS = 60L * 60L * 1000L
private const val LIVE_UPDATE_POPUP_DURATION_MILLIS = 4_000L
