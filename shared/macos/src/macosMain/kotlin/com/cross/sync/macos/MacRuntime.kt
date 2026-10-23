package com.cross.sync.macos

import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.usecase.*
import com.cross.sync.clipboard.presentation.ClipboardViewModel
import androidx.lifecycle.ViewModel
import com.cross.sync.core.db.AppDatabase
import com.cross.sync.core.db.createNativeDatabase
import com.cross.sync.notifications.di.notificationModule
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.usecase.*
import com.cross.sync.notifications.presentation.NotificationViewModel
import com.cross.sync.setting.domain.DesktopAutoStartManager
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.setting.presentation.DesktopSettingViewModel
import com.cross.sync.syncing.data.repository.DeviceRepositoryImpl
import com.cross.sync.syncing.domain.repository.ClipboardServer
import com.cross.sync.syncing.domain.repository.DeviceRepository
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.entity.ServerState
import com.cross.sync.syncing.domain.usecases.*
import com.cross.sync.syncing.network.LocalServer
import com.cross.sync.syncing.network.ServerNetworkInfo
import com.cross.sync.syncing.runtime.DesktopRuntime
import com.cross.sync.syncing.runtime.NotificationDeliveryCoordinator
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

interface SystemNotificationSink {
    fun publish(notification: SyncedNotification)
    fun withdraw(id: String)
}

data class MacRuntimeState(
    val serverError: String? = null,
    val connected: Boolean = false,
    val popups: List<SyncedNotification> = emptyList(),
    val liveUpdate: SyncedNotification? = null,
)

class MacRuntime(desktop: DesktopServices, crypto: CryptoOperations, system: SystemNotificationSink,
                 dataDirectory: String, settingsSuite: String, listenPort: Int) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(MacRuntimeState())
    private val application: KoinApplication
    private val runtime: DesktopRuntime
    private var closed = false
    private val screens = mutableSetOf<ScreenOwner>()
    val current: MacRuntimeState get() = state.value
    init {
        val settings = NSUserDefaultsSettings(NSUserDefaults(suiteName = settingsSuite))
        val preferences = SettingPreferencesStore(settings)
        val clipboard = NativeClipboardManager(desktop, preferences, scope)
        application = koinApplication {
            modules(clipboardModule, notificationModule, module {
                single<AppDatabase> { createNativeDatabase(dataDirectory) }
                single<Settings> { settings }
                single { preferences }
                single<ClipboardManager> { clipboard }
                single<DeviceRepository> { DeviceRepositoryImpl(get()) }
                single<ClipboardServer> {
                    LocalServer(NativeCryptoEngine(crypto), get(), get(), get(), NativeTokens(crypto, settings),
                        object : ServerNetworkInfo {
                            override fun addresses() = desktop.lanAddresses()
                            override fun name() = desktop.deviceName()
                        }, listenPort, settings)
                }
                single<DesktopAutoStartManager> {
                    object : DesktopAutoStartManager {
                        override fun isSupported() = true
                        override fun isEnabled() = desktop.autoStartEnabled()
                        override fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
                            desktop.setAutoStart(enabled)?.let { error(it) }
                        }
                    }
                }
            })
        }
        val koin = application.koin
        val delivery = NotificationDeliveryCoordinator(
            delivery = { preferences.getGeneralSettings().desktopNotificationDelivery },
            system = system::publish,
            popup = { notification -> state.update { it.copy(popups = it.popups.filterNot { existing -> key(existing) == key(notification) } + notification) } },
            hide = { id ->
                system.withdraw(id)
                state.update { it.copy(popups = it.popups.filterNot { notification -> key(notification) == id }) }
            },
            liveUpdate = { notification -> state.update { it.copy(liveUpdate = notification) } },
        )
        runtime = DesktopRuntime(koin.get(), koin.get(),
            StartSyncUseCase(koin.get(), koin.get(), koin.get(), koin.get(), koin.get()),
            koin.get(), koin.get(), preferences, koin.get(), delivery)
        scope.launch {
            koin.get<SaveApplicationsUseCase>()(desktop.installedApplications().map { Application(it.id, it.name, it.icon, it.path) })
        }
        scope.launch {
            koin.get<ClipboardServer>().observeServerState().collect { server ->
                state.update { it.copy(serverError = (server as? ServerState.Error)?.message) }
            }
        }
        scope.launch {
            koin.get<ClipboardServer>().observePairingState().collect { pairing ->
                state.update { it.copy(connected = pairing is PairingState.Connected) }
            }
        }
        runtime.start()
    }
    fun watch(receive: (MacRuntimeState) -> Unit): StateSubscription {
        receive(state.value)
        return StateSubscription(scope.launch { state.collect { receive(it) } })
    }
    fun clipboardScreen(): ClipboardScreenModel {
        val k = application.koin
        val model = ClipboardViewModel(k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), k.get(), initializeClipboard = false)
        return ClipboardScreenModel(model, owner(model))
    }
    fun settingsScreen(): SettingsScreenModel {
        val k = application.koin
        val model = DesktopSettingViewModel(k.get(), k.get(), k.get(), k.get(), AddDeviceUseCase(k.get()), DeleteDeviceUseCase(k.get(), k.get()), ObserveDevicesUseCase(k.get()), k.get(), k.get(), k.get())
        return SettingsScreenModel(model, owner(model))
    }
    fun notificationsScreen(): NotificationsScreenModel {
        val k = application.koin
        val model = NotificationViewModel(k.get(), k.get(), k.get(), SendNotificationActionUseCase(k.get()), k.get())
        return NotificationsScreenModel(model, owner(model))
    }
    fun dismissPopup(id: String) { state.update { it.copy(popups = it.popups.filterNot { n -> key(n) == id }) } }
    private fun owner(viewModel: ViewModel): ScreenOwner = ScreenOwner(viewModel).also { owner ->
        check(!closed)
        screens += owner
        scope.launch {
            owner.awaitClosed()
            screens -= owner
        }
    }
    fun close(completed: () -> Unit) {
        if (closed) return
        closed = true
        val owners = screens.toList()
        owners.forEach { it.close() }
        scope.cancel()
        val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        cleanup.launch {
            owners.forEach { it.awaitClosed() }
            scope.coroutineContext[kotlinx.coroutines.Job]?.join()
            runtime.shutdown()
            application.koin.get<AppDatabase>().close()
            application.close()
            screens.clear()
            completed()
            cleanup.cancel()
        }
    }
    private fun key(notification: SyncedNotification) = "${notification.deviceId}:${notification.notificationKey}"
}
