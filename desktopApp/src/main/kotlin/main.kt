import notifications.MacOsSystemNotificationCenter
import androidx.compose.runtime.DisposableEffect
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.runtime.DesktopRuntime
import com.cross.sync.syncing.runtime.NotificationDeliveryCoordinator
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
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import utils.getInstalledApplications
import windows.GeneralWindow
import windows.DesktopUiResources
import windows.GeneralWindowState
import windows.HomeTab
import windows.QuickClipboardWindow
import windows.MacOsLiveUpdateChip
import kotlin.time.ExperimentalTime
import notifications.MacOsLiveUpdateStatusItem
import windows.MacOsNotificationPopup

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyManager)
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
fun main() {
    DesktopUiResources.configureImageLoader()
    crossSyncApplication()
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
private fun crossSyncApplication() = application {
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
        val clipboardServer = koinInject<ClipboardServer>()
        val initClipboard = koinInject<InitClipboardManagerUseCase>()
        val observePairingUseCase = koinInject<ObservePairingUseCase>()
        val saveApplicationsUseCase = koinInject<SaveApplicationsUseCase>()
        val clearCopiedDataOlderThanUseCase = koinInject<ClearCopiedDataOlderThanUseCase>()
        val clearNotificationHistoryUseCase = koinInject<ClearNotificationHistoryUseCase>()
        val settingPreferencesStore = koinInject<SettingPreferencesStore>()
        val globalHotkeyManager = koinInject<GlobalHotkeyManager>()
        val observeNotificationsUseCase = koinInject<ObserveNotificationsUseCase>()
        var liveUpdateNotification by remember { mutableStateOf<SyncedNotification?>(null) }
        var generalWindowShowed by remember { mutableStateOf<GeneralWindowState?>(null) }
        var selectedHomeTab by remember { mutableStateOf(HomeTab.CLIPBOARD) }
        var popupNotifications by remember { mutableStateOf<List<SyncedNotification>>(emptyList()) }
        val notificationBannerCoordinator = remember {
            NotificationDeliveryCoordinator(
                popup = { notification ->
                    popupNotifications = popupNotifications
                        .filterNot { current ->
                            current.deviceId == notification.deviceId &&
                                current.notificationKey == notification.notificationKey
                        }
                        .plus(notification)
                },
                hide = { id ->
                    popupNotifications = popupNotifications.filterNot {
                        "${it.deviceId}:${it.notificationKey}" == id
                    }
                },
                delivery = {
                    settingPreferencesStore.getGeneralSettings().desktopNotificationDelivery
                },
                system = { MacOsSystemNotificationCenter().publish(it) },
                liveUpdate = { liveUpdateNotification = it },
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

        val desktopRuntime = remember {
            DesktopRuntime(clipboardServer, initClipboard, startSyncUseCase,
                clearCopiedDataOlderThanUseCase, clearNotificationHistoryUseCase,
                settingPreferencesStore, observeNotificationsUseCase, notificationBannerCoordinator)
        }
        DisposableEffect(desktopRuntime) {
            desktopRuntime.start()
            onDispose { desktopRuntime.close() }
        }

        LaunchedEffect(
            liveUpdateNotification?.deviceId,
            liveUpdateNotification?.notificationKey,
            liveUpdateNotification?.kind,
        ) {
            showLiveUpdateChip = liveUpdateNotification != null
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
            onClick = { showLiveUpdateChip = !showLiveUpdateChip },
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
