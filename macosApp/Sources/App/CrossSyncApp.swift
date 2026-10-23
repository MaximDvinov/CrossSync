import SwiftUI
@preconcurrency import CrossSyncShared

@main
struct CrossSyncApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    var body: some Scene {
        Window("CrossSync", id: "main") {
            MainScene(delegate: delegate)
        }
        .defaultSize(width: 620, height: 800)
        .defaultLaunchBehavior(.presented)
        Settings { SettingsWindow(environment: AppEnvironment.shared) }
            .commands {
                CommandGroup(after: .appInfo) {
                    Button("Open CrossSync") { delegate.openMain() }
                    Button("Quick Access") { delegate.openQuickAccess() }
                        .keyboardShortcut("v", modifiers: [.command, .shift])
                }
                CommandGroup(replacing: .appSettings) {
                    Button("Settings…") { delegate.openSettings() }.keyboardShortcut(",", modifiers: .command)
                }
            }
    }
}

private struct MainScene: View {
    let delegate: AppDelegate
    @Environment(\.openWindow) private var openWindow
    @Environment(\.openSettings) private var openSettings
    var body: some View {
        MainWindow(environment: AppEnvironment.shared)
            .onAppear {
                delegate.openMainAction = { openWindow(id: "main"); NSApp.activate(ignoringOtherApps: true) }
                delegate.openSettingsAction = { openSettings(); NSApp.activate(ignoringOtherApps: true) }
            }
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    var openMainAction: (() -> Void)?
    var openSettingsAction: (() -> Void)?
    private var commands: [NSObjectProtocol] = []
    private var statusItem: NSStatusItem?
    private var quickAccess: QuickAccessController?
    private var shortcut: GlobalShortcut?
    private var panels: NotificationPanels?
    private var systemActions: NotificationsStore?
    private var subscription: StateSubscription?
    private var systemActionSubscription: StateSubscription?
    private var lastSystemActionError: String?

    func applicationDidFinishLaunching(_ notification: Notification) {
        let environment = AppEnvironment.shared
        commands = [
            NotificationCenter.default.addObserver(forName: .openCrossSync, object: nil, queue: .main) { [weak self] _ in MainActor.assumeIsolated { self?.openMain() } },
            NotificationCenter.default.addObserver(forName: .openCrossSyncSettings, object: nil, queue: .main) { [weak self] _ in MainActor.assumeIsolated { self?.openSettings() } }
        ]
        quickAccess = QuickAccessController(environment: environment)
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        item.button?.image = NSImage(systemSymbolName: "doc.on.clipboard", accessibilityDescription: "CrossSync")
        item.button?.target = self; item.button?.action = #selector(statusClicked)
        item.button?.sendAction(on: [.leftMouseUp, .rightMouseUp])
        statusItem = item
        shortcut = GlobalShortcut { [weak self] in self?.quickAccess?.toggle() }
        if shortcut?.register() == false { item.button?.toolTip = "⌘⇧V is unavailable. Open Quick Access from this menu." }
        panels = NotificationPanels(environment: environment)
        systemActions = NotificationsStore(runtime: environment.runtime)
        systemActionSubscription = systemActions?.model.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, state.actionError != self.lastSystemActionError else { return }
                self.lastSystemActionError = state.actionError
                if let error = state.actionError {
                    environment.systemActionError = error
                    self.openMain()
                }
            }
        }
        environment.notifications.onAction = { [weak self] id, index, text in
            guard let store = self?.systemActions,
                  let notification = store.state.notifications.first(where: { notificationID($0) == id }) else { self?.openMain(); return }
            if let index, let text { store.model.reply(notification: notification, index: index, text: text) }
            else if let index { store.model.invoke(notification: notification, index: index) }
            else { store.model.open(notification: notification); self?.openMain() }
        }
        subscription = environment.runtime.watch { [weak self] state in
            MainActor.assumeIsolated { self?.statusItem?.button?.appearsDisabled = !state.connected }
        }
    }
    @objc private func statusClicked() {
        if NSApp.currentEvent?.type == .rightMouseUp {
            let menu = NSMenu()
            for (title, action) in [("Open CrossSync", #selector(openMain)), ("Notifications", #selector(openNotifications)), ("Settings…", #selector(openSettings)), ("Quit CrossSync", #selector(quit))] {
                let item = NSMenuItem(title: title, action: action, keyEquivalent: title == "Quit CrossSync" ? "q" : "")
                item.target = self; menu.addItem(item)
            }
            statusItem?.menu = menu
            statusItem?.button?.performClick(nil)
            statusItem?.menu = nil
        } else { quickAccess?.toggle() }
    }
    @objc func openMain() { openMainAction?() }
    @objc func openQuickAccess() { quickAccess?.toggle() }
    @objc private func openNotifications() { quickAccess?.toggle(notifications: true) }
    @objc func openSettings() { openSettingsAction?() }
    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        if !flag { openMain() }
        return true
    }
    @objc private func quit() { NSApp.terminate(nil) }
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }
    func applicationShouldTerminate(_ sender: NSApplication) -> NSApplication.TerminateReply {
        commands.forEach(NotificationCenter.default.removeObserver)
        subscription?.cancel(); systemActionSubscription?.cancel(); systemActions?.close(); systemActions = nil
        shortcut?.stop(); quickAccess?.hide(); panels?.close()
        AppEnvironment.shared.close { NSApp.reply(toApplicationShouldTerminate: true) }
        return .terminateLater
    }
}
