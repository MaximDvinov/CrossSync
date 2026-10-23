import SwiftUI
@preconcurrency import CrossSyncShared

@MainActor
final class NotificationPanels {
    private let environment: AppEnvironment
    private var popup: NSPanel?
    private var livePanel: NSPanel?
    private var liveItem: NSStatusItem?
    private var subscription: StateSubscription?
    init(environment: AppEnvironment) {
        self.environment = environment
        subscription = environment.runtime.watch { [weak self] value in
            MainActor.assumeIsolated { self?.update(value) }
        }
    }
    private func update(_ state: MacRuntimeState) {
        if state.popups.isEmpty {
            popup?.orderOut(nil); popup?.contentView = nil; popup = nil
        } else {
            if popup == nil {
                popup = makePanel(width: 380, height: 480)
                let content = NSHostingView(rootView: PopupNotifications(environment: environment))
                content.sizingOptions = []
                popup?.contentView = content
            }
            popup?.orderFrontRegardless()
        }
        if let notification = state.liveUpdate {
            if liveItem == nil {
                liveItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
                liveItem?.button?.target = self
                liveItem?.button?.action = #selector(toggleLive)
            }
            let title = notification.hasPreview ? (notification.shortCriticalText.isEmpty ? notification.title : notification.shortCriticalText) : notification.appName
            liveItem?.button?.title = String(title.prefix(35))
            liveItem?.button?.image = NSImage(systemSymbolName: "bolt.circle", accessibilityDescription: "Live update")
        } else {
            if let liveItem { NSStatusBar.system.removeStatusItem(liveItem) }
            liveItem = nil; livePanel?.orderOut(nil); livePanel?.contentView = nil; livePanel = nil
        }
    }
    @objc private func toggleLive() {
        if livePanel?.isVisible == true { livePanel?.orderOut(nil) }
        else if environment.state.liveUpdate != nil { showLive() }
    }
    private func showLive() {
        if livePanel == nil {
            livePanel = makePanel(width: 380, height: 280)
            let content = NSHostingView(rootView: LiveNotification(environment: environment))
            content.sizingOptions = []
            livePanel?.contentView = content
        }
        livePanel?.orderFrontRegardless()
    }
    private func makePanel(width: CGFloat, height: CGFloat) -> NSPanel {
        let panel = NSPanel(contentRect: NSRect(x: 0, y: 0, width: width, height: height), styleMask: [.titled, .fullSizeContentView, .nonactivatingPanel], backing: .buffered, defer: false)
        panel.titleVisibility = .hidden; panel.titlebarAppearsTransparent = true
        panel.level = .floating; panel.hidesOnDeactivate = false
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        let screen = NSScreen.screens.first { NSMouseInRect(NSEvent.mouseLocation, $0.frame, false) } ?? NSScreen.main
        let visible = screen?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1000, height: 800)
        panel.setFrameOrigin(NSPoint(x: visible.maxX - width - 12, y: visible.maxY - height - 12))
        return panel
    }
    func close() {
        subscription?.cancel(); popup?.close(); livePanel?.close()
        if let liveItem { NSStatusBar.system.removeStatusItem(liveItem) }
    }
}

private struct PopupNotifications: View {
    let environment: AppEnvironment
    @State private var store: NotificationsStore?
    var body: some View {
        ScrollView {
            if let store {
                VStack(spacing: 10) {
                    HStack {
                        Text("Notifications").font(.headline)
                        Spacer()
                        Button("Dismiss All") {
                            for notification in environment.state.popups {
                                environment.runtime.dismissPopup(id: notificationID(notification))
                            }
                        }
                    }
                    ForEach(environment.state.popups.map(NotificationItem.init)) { item in
                        let notification = item.notification
                        NotificationCard(notification: notification, state: store.state, onEvent: store.send, popupDismiss: { environment.runtime.dismissPopup(id: notificationID(notification)) })
                    }
                }.padding()
            }
        }
        .preferredColorScheme(environment.appearance)
        .onAppear { store = NotificationsStore(runtime: environment.runtime) }
        .onDisappear { store?.close(); store = nil }
    }
}

private struct LiveNotification: View {
    let environment: AppEnvironment
    @State private var store: NotificationsStore?
    var body: some View {
        Group {
            if let store, let notification = environment.state.liveUpdate {
                NotificationCard(notification: notification, state: store.state, onEvent: store.send).padding()
            }
        }
            .preferredColorScheme(environment.appearance)
            .onAppear { store = NotificationsStore(runtime: environment.runtime) }
            .onDisappear { store?.close(); store = nil }
    }
}
