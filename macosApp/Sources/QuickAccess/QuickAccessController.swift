import SwiftUI
@preconcurrency import CrossSyncShared

@MainActor
final class QuickAccessController {
    private var panel: NSPanel?
    private var previousApplication: NSRunningApplication?
    private let environment: AppEnvironment
    init(environment: AppEnvironment) { self.environment = environment }
    func toggle(notifications: Bool = false) {
        if let panel, panel.isVisible { hide(); return }
        previousApplication = NSWorkspace.shared.frontmostApplication
        let mouse = NSEvent.mouseLocation
        let visible = NSScreen.screens.first(where: { NSMouseInRect(mouse, $0.frame, false) })?.visibleFrame ?? NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1000, height: 800)
        let panel = QuickAccessPanel(contentRect: NSRect(x: 0, y: 0, width: min(440, visible.width - 16), height: min(560, visible.height - 16)), styleMask: [.borderless, .nonactivatingPanel], backing: .buffered, defer: false)
        panel.onCancel = { [weak self] in self?.hide() }
        panel.becomesKeyOnlyIfNeeded = false
        panel.title = "CrossSync"
        panel.isOpaque = false; panel.backgroundColor = .clear; panel.hasShadow = true
        panel.level = .floating; panel.isFloatingPanel = true
        panel.hidesOnDeactivate = false; panel.isReleasedWhenClosed = false
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        let content = NSHostingView(rootView: QuickAccessView(environment: environment, showNotifications: notifications, close: { [weak self] in self?.hide() }, paste: { [weak self] in self?.paste() }))
        content.sizingOptions = []
        panel.contentView = content
        panel.setFrameOrigin(Self.position(mouse: mouse, visible: visible, size: panel.frame.size))
        self.panel = panel
        panel.makeKeyAndOrderFront(nil)
    }
    func hide(restoreFocus: Bool = true) {
        panel?.orderOut(nil); panel?.contentView = nil; panel = nil
        if restoreFocus { previousApplication?.activate(options: []) }
    }
    private func paste() {
        hide(restoreFocus: false); NativePaste.paste(into: previousApplication)
    }
    static func position(mouse: NSPoint, visible: NSRect, size: NSSize) -> NSPoint {
        NSPoint(x: max(visible.minX + 8, min(mouse.x - size.width / 2, visible.maxX - size.width - 8)),
                y: max(visible.minY + 8, min(mouse.y - size.height, visible.maxY - size.height - 8)))
    }
}

@MainActor
private final class QuickAccessPanel: NSPanel {
    var onCancel: (() -> Void)?
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { false }
    override func cancelOperation(_ sender: Any?) { onCancel?() }
    override func performKeyEquivalent(with event: NSEvent) -> Bool {
        if event.keyCode == 53 { onCancel?(); return true }
        return super.performKeyEquivalent(with: event)
    }
}

private struct QuickAccessView: View {
    let environment: AppEnvironment
    @State var showNotifications: Bool
    let close: () -> Void
    let paste: () -> Void
    @State private var clipboard: ClipboardStore?
    @State private var notifications: NotificationsStore?
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                if showNotifications {
                    Button { showNotifications = false } label: { Image(systemName: "chevron.left") }
                        .buttonStyle(.plain).help("Back to clipboard")
                }
                Button {
                    close(); NotificationCenter.default.post(name: .openCrossSync, object: nil)
                } label: {
                    Label("CrossSync", systemImage: "doc.on.clipboard")
                        .font(.system(size: 14, weight: .semibold)).foregroundStyle(.tint)
                }.buttonStyle(.plain).help("Open CrossSync")
                Spacer(minLength: 0)
                if !showNotifications && !NativePaste.isTrusted {
                    Menu {
                        Button("Enable Automatic Paste…") { NativePaste.requestAccessibility() }
                    } label: { Image(systemName: "ellipsis") }
                        .menuStyle(.borderlessButton).fixedSize().help("Paste options")
                }
                Button { showNotifications.toggle() } label: { Image(systemName: "bell") }
                    .buttonStyle(.plain).help("Notifications")
                Button {
                    close(); NotificationCenter.default.post(name: .openCrossSyncSettings, object: nil)
                } label: { Image(systemName: "gearshape") }.buttonStyle(.plain).help("Settings")
                Button(action: close) { Image(systemName: "xmark") }
                    .buttonStyle(.plain).help("Close (Escape)").accessibilityLabel("Close")
            }.controlSize(.small).padding(.horizontal, 10).padding(.vertical, 9)
            Divider()
            if showNotifications, let notifications {
                NotificationsView(state: notifications.state, onEvent: notifications.send, compact: true)
            } else if let clipboard {
                ClipboardView(state: clipboard.state, onEvent: clipboard.send, limit: environment.quickAccessSize, paste: paste, compact: true)
            }
        }
        .background(Color(nsColor: .windowBackgroundColor))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .preferredColorScheme(environment.appearance)
        .onAppear {
            clipboard = ClipboardStore(runtime: environment.runtime)
            notifications = NotificationsStore(runtime: environment.runtime)
        }
        .onDisappear { clipboard?.close(); notifications?.close(); clipboard = nil; notifications = nil }
        .onExitCommand(perform: close)
    }
}

extension Notification.Name {
    static let openCrossSync = Notification.Name("CrossSync.OpenMain")
    static let openCrossSyncSettings = Notification.Name("CrossSync.OpenSettings")
}
