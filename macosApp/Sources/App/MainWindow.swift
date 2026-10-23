import SwiftUI
@preconcurrency import CrossSyncShared

private enum MainSection { case clipboard, notifications, settings }

struct MainWindow: View {
    let environment: AppEnvironment
    @Environment(\.dismiss) private var dismiss
    @State private var clipboard: ClipboardStore?
    @State private var notifications: NotificationsStore?
    @State private var settings: SettingsStore?
    @State private var section = MainSection.clipboard
    @State private var previousSection = MainSection.clipboard

    var body: some View {
        VStack(spacing: 0) {
            header
            if let error = environment.state.serverError { errorBanner(error) }
            if let error = environment.systemActionError {
                errorBanner(error, dismiss: { environment.systemActionError = nil })
            }
            content.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color(nsColor: .windowBackgroundColor))
        .frame(minWidth: 520, minHeight: 500)
        .preferredColorScheme(environment.appearance)
        .onAppear {
            if clipboard == nil { clipboard = ClipboardStore(runtime: environment.runtime) }
            if notifications == nil { notifications = NotificationsStore(runtime: environment.runtime) }
            if settings == nil { settings = SettingsStore(runtime: environment.runtime) }
        }
        .onDisappear {
            clipboard?.close(); notifications?.close(); settings?.close()
            clipboard = nil; notifications = nil; settings = nil
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            if section != .clipboard {
                Button { section = section == .settings ? previousSection : .clipboard }
                    label: { Image(systemName: "chevron.left") }
                    .buttonStyle(.bordered).buttonBorderShape(.circle).help("Back")
            }
            Button { section = .clipboard } label: {
                Label("CrossSync", systemImage: "doc.on.clipboard")
                    .font(.title3.weight(.semibold)).foregroundStyle(.tint)
            }.buttonStyle(.plain)
            Spacer()
            Image(systemName: environment.state.connected ? "checkmark.circle.fill" : "circle.dashed")
                .foregroundStyle(.secondary).help(environment.state.connected ? "Device connected" : "No device connected")
            Button { section = .notifications } label: { Image(systemName: "bell") }
                .buttonStyle(.bordered).buttonBorderShape(.circle).help("Notifications")
            Button {
                if section != .settings { previousSection = section }
                section = .settings
            } label: { Image(systemName: "gearshape") }
                .buttonStyle(.bordered).buttonBorderShape(.circle).help("Settings")
            Button { dismiss() } label: { Image(systemName: "xmark") }
                .buttonStyle(.bordered).buttonBorderShape(.circle).help("Close window")
        }.padding(.horizontal, 20).padding(.vertical, 12)
    }

    @ViewBuilder private var content: some View {
        switch section {
        case .clipboard:
            if let clipboard { ClipboardView(state: clipboard.state, onEvent: clipboard.send) }
            else { ProgressView() }
        case .notifications:
            if let notifications { NotificationsView(state: notifications.state, onEvent: notifications.send) }
            else { ProgressView() }
        case .settings:
            if let settings { SettingsView(state: settings.state, onEvent: settings.send) }
            else { ProgressView() }
        }
    }

    private func errorBanner(_ message: String, dismiss: (() -> Void)? = nil) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Label(message, systemImage: "exclamationmark.triangle").font(.callout)
            Spacer()
            if let dismiss { Button("Dismiss", action: dismiss) }
        }.padding(12).frame(maxWidth: .infinity, alignment: .leading).background(.quaternary)
    }
}
