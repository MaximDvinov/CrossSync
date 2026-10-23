import SwiftUI
import Observation
import ServiceManagement
@preconcurrency import CrossSyncShared

enum ClipboardEvent {
    case selectCategory(Int64)
    case copy(Int64, @MainActor (String?) -> Void)
    case delete(Int64)
    case move(Int64, Int64)
    case clear
}

enum NotificationsEvent {
    case dismiss(SyncedNotification)
    case open(SyncedNotification)
    case invoke(SyncedNotification, Int32)
    case reply(SyncedNotification, Int32, String)
    case clear
}

enum SettingsEvent {
    case autoStart(Bool), theme(String), retention(Int32), quickSize(Int32)
    case notificationRetention(Int32), delivery(String)
    case addCategory(String), renameCategory(Int64, String), deleteCategory(Int64)
    case exclude(String), include(String), pair, cancelPairing, unpair(String)
    case requestNotifications(@MainActor @Sendable (String) -> Void)
    case enablePaste, openPermissions(Bool), openLoginItems
}

@MainActor @Observable
final class ClipboardStore {
    let model: ClipboardScreenModel
    var state: ClipboardState
    private var subscription: StateSubscription?
    init(runtime: MacRuntime) {
        model = runtime.clipboardScreen()
        state = model.current
        subscription = model.watch { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
    }
    func send(_ event: ClipboardEvent) {
        switch event {
        case .selectCategory(let id): model.selectCategory(id: id)
        case .copy(let id, let completed):
            model.doCopy(id: id) { error in MainActor.assumeIsolated { completed(error) } }
        case .delete(let id): model.delete(id: id)
        case .move(let id, let category): model.moveToCategory(id: id, categoryId: category)
        case .clear: model.clear()
        }
    }
    func close() {
        guard subscription != nil else { return }
        subscription?.cancel(); subscription = nil; model.close()
    }
    isolated deinit { close() }
}

@MainActor @Observable
final class NotificationsStore {
    let model: NotificationsScreenModel
    var state: NotificationState
    private var subscription: StateSubscription?
    init(runtime: MacRuntime) {
        model = runtime.notificationsScreen()
        state = model.current
        subscription = model.watch { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
    }
    func send(_ event: NotificationsEvent) {
        switch event {
        case .dismiss(let notification): model.dismiss(notification: notification)
        case .open(let notification): model.open(notification: notification)
        case .invoke(let notification, let index): model.invoke(notification: notification, index: index)
        case .reply(let notification, let index, let text): model.reply(notification: notification, index: index, text: text)
        case .clear: model.clear()
        }
    }
    func close() {
        guard subscription != nil else { return }
        subscription?.cancel(); subscription = nil; model.close()
    }
    isolated deinit { close() }
}

@MainActor @Observable
final class SettingsStore {
    let model: SettingsScreenModel
    var state: SettingState
    private var subscription: StateSubscription?
    init(runtime: MacRuntime) {
        model = runtime.settingsScreen()
        state = model.current
        subscription = model.watch { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
    }
    func send(_ event: SettingsEvent) {
        switch event {
        case .autoStart(let enabled): model.setAutoStart(enabled: enabled)
        case .theme(let value): model.setTheme(value: value)
        case .retention(let days): model.setRetention(days: days)
        case .quickSize(let size): model.setQuickSize(size: size)
        case .notificationRetention(let days): model.setNotificationRetention(days: days)
        case .delivery(let value): model.setDelivery(value: value)
        case .addCategory(let name): model.addCategory(name: name)
        case .renameCategory(let id, let name): model.renameCategory(id: id, name: name)
        case .deleteCategory(let id): model.deleteCategory(id: id)
        case .exclude(let id): model.exclude(id: id)
        case .include(let id): model.include(id: id)
        case .pair: model.pair()
        case .cancelPairing: model.cancelPairing()
        case .unpair(let id): model.unpair(id: id)
        case .requestNotifications(let completed): AppEnvironment.shared.notifications.requestAuthorization(completed: completed)
        case .enablePaste: NativePaste.requestAccessibility()
        case .openPermissions(let notifications):
            if notifications, let url = URL(string: "x-apple.systempreferences:com.apple.preference.notifications") {
                NSWorkspace.shared.open(url)
            } else { NativePaste.openAccessibilitySettings() }
        case .openLoginItems: SMAppService.openSystemSettingsLoginItems()
        }
    }
    func close() {
        guard subscription != nil else { return }
        subscription?.cancel(); subscription = nil; model.close()
    }
    isolated deinit { close() }
    var colorScheme: ColorScheme? {
        switch state.themeMode.name {
        case "LIGHT": .light
        case "DARK": .dark
        default: nil
        }
    }
}

@MainActor @Observable
final class AppEnvironment {
    static let shared = AppEnvironment()
    let desktop = MacDesktopServices()
    let crypto = CryptoKitOperations()
    let notifications = SystemNotifications()
    let runtime: MacRuntime
    var state: MacRuntimeState
    var appearance: ColorScheme?
    var systemActionError: String?
    private var subscription: StateSubscription?
    private let appearanceModel: SettingsScreenModel
    private var appearanceSubscription: StateSubscription?
    private init() {
        runtime = MacRuntime(desktop: desktop, crypto: crypto, system: notifications,
                             dataDirectory: MacDesktopServices.dataDirectory.path, settingsSuite: "com.cross.sync.macos.preferences", listenPort: 33333)
        state = runtime.current
        appearanceModel = runtime.settingsScreen()
        subscription = runtime.watch { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
        appearanceSubscription = appearanceModel.watch { [weak self] value in
            MainActor.assumeIsolated {
                self?.appearance = value.themeMode.name == "LIGHT" ? .light : value.themeMode.name == "DARK" ? .dark : nil
            }
        }
    }
    var quickAccessSize: Int { Int(appearanceModel.current.quickAccessHistorySize) }
    func close(completed: @escaping () -> Void) {
        subscription?.cancel(); appearanceSubscription?.cancel()
        appearanceModel.close(); runtime.close(completed: completed)
    }
}
