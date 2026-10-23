import SwiftUI
@preconcurrency import CrossSyncShared

struct NotificationsView: View {
    let state: NotificationState
    let onEvent: (NotificationsEvent) -> Void
    var compact = false
    @State private var archive = false
    @State private var searching = false
    @State private var search = ""
    private var showsArchive: Bool { archive }
    @State private var clearing = false
    private var groups: [NotificationGroup] {
        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)
        let visible = state.notifications.filter {
            (showsArchive ? !$0.isActive : $0.isActive) &&
                (query.isEmpty || $0.appName.localizedCaseInsensitiveContains(query) ||
                 ($0.hasPreview && ($0.title.localizedCaseInsensitiveContains(query) || $0.body.localizedCaseInsensitiveContains(query))))
        }
        let grouped = Dictionary(grouping: visible) { notification in
            notification.groupKey.isEmpty ? notificationID(notification) : "\(notification.deviceId):\(notification.packageName):\(notification.groupKey)"
        }
        return grouped.values.map { entries in
            let children = entries.filter { !$0.isGroupSummary }
            return NotificationGroup(notifications: (children.isEmpty ? entries : children).sorted { $0.updatedAt > $1.updatedAt })
        }.sorted { ($0.notifications.first?.updatedAt ?? 0) > ($1.notifications.first?.updatedAt ?? 0) }
    }
    private var items: [SyncedNotification] { groups.flatMap(\.notifications) }

    var body: some View {
        VStack(spacing: 0) {
            if let error = state.actionError {
                Label(error, systemImage: "exclamationmark.triangle").font(.callout)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(14).background(.quaternary)
            }
            HStack(spacing: 8) {
                Picker("Notifications", selection: $archive) {
                    Text("Active").tag(false)
                    Text("History").tag(true)
                }.pickerStyle(.segmented).labelsHidden().frame(width: compact ? 180 : 220)
                Spacer(minLength: 0)
                Button { searching.toggle(); if !searching { search = "" } } label: { Image(systemName: "magnifyingglass") }
                    .buttonStyle(.borderless).help("Search notifications")
                Button { clearing = true } label: { Image(systemName: "trash") }
                    .buttonStyle(.borderless).disabled(state.notifications.isEmpty).help("Clear history")
            }.controlSize(compact ? .small : .regular).padding(.horizontal, compact ? 10 : 20).padding(.vertical, compact ? 6 : 10)
            if searching {
                TextField("Search notifications", text: $search).textFieldStyle(.roundedBorder)
                    .padding(.horizontal, compact ? 10 : 20).padding(.bottom, 8)
            }
            cardList
            Divider()
            HStack {
                Text("\(items.count) \(showsArchive ? "archived" : "active") notifications").font(.caption).foregroundStyle(.secondary)
                Spacer()
            }.padding(.horizontal, compact ? 10 : 20).padding(.vertical, 6)
        }
        .confirmationDialog("Clear notification history?", isPresented: $clearing) {
            Button("Clear", role: .destructive) { onEvent(.clear) }
        } message: { Text("This removes the local history for all devices, including notifications hidden by search.") }
    }

    private var empty: some View {
        ContentUnavailableView(search.isEmpty ? (showsArchive ? "No archived notifications" : "You're all caught up") : "No results",
            systemImage: search.isEmpty ? "bell" : "magnifyingglass",
            description: Text(search.isEmpty ? (showsArchive ? "Past notifications will appear here." : "Notifications appear when your Android device is connected.") : "Try another search."))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder private var cardList: some View {
        if groups.isEmpty { empty }
        else {
            ScrollView {
                LazyVStack(spacing: compact ? 8 : 12) {
                    ForEach(groups) { entry in
                        let group = entry.notifications
                        if group.count > 1 {
                            DisclosureGroup("\(group[0].appName) · \(group.count)") {
                                ForEach(group.map(NotificationItem.init)) { item in
                                    card(item.notification)
                                }
                            }
                        } else if let notification = group.first { card(notification) }
                    }
                }.padding(.horizontal, compact ? 10 : 20).padding(.bottom, 12)
            }
        }
    }

    private func card(_ notification: SyncedNotification) -> some View {
        NotificationCard(notification: notification, state: state, onEvent: onEvent)
            .padding(.horizontal, 12).padding(.vertical, 4)
            .background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(.secondary.opacity(0.12), lineWidth: 0.5))
    }

}

struct NotificationCard: View {
    let notification: SyncedNotification
    let state: NotificationState
    let onEvent: (NotificationsEvent) -> Void
    var popupDismiss: (() -> Void)? = nil
    @State private var reply = ""
    @State private var replyIndex: Int32?
    private var pending: Bool { state.pendingActions.contains(notificationID(notification)) }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                NotificationImage(base64: notification.media?.appIcon, size: 24)
                Text(notification.appName).font(.headline).lineLimit(1)
                if !notification.isRead { Circle().fill(.tint).frame(width: 6, height: 6).accessibilityLabel("Unread") }
                Spacer()
                Text(Date(timeIntervalSince1970: Double(notification.postedAt) / 1000), style: .time).font(.caption).foregroundStyle(.secondary)
                Button {
                    if let popupDismiss { popupDismiss() } else { onEvent(.dismiss(notification)) }
                } label: { Image(systemName: "xmark") }.buttonStyle(.plain).help("Dismiss")
            }
            HStack(alignment: .top) {
                if notification.hasPreview && notification.media?.avatar != nil { NotificationImage(base64: notification.media?.avatar, size: 36) }
                VStack(alignment: .leading, spacing: 4) {
                    Text(notification.hasPreview ? notification.title : "Content hidden").font(.headline)
                    if notification.hasPreview { Text(notification.body).textSelection(.enabled) }
                }
            }
            if notification.hasPreview, let base64 = notification.media?.image, let data = Data(base64Encoded: base64), let image = NSImage(data: data) {
                Image(nsImage: image).resizable().scaledToFit().frame(maxHeight: 180).clipShape(RoundedRectangle(cornerRadius: 8))
            }
            if notification.isActive {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 125))], alignment: .leading) {
                    Button("Open on Android") { onEvent(.open(notification)) }.disabled(pending)
                    ForEach(notification.actions, id: \.index) { action in
                        Button(action.title) {
                            if action.supportsReply { replyIndex = action.index } else { onEvent(.invoke(notification, action.index)) }
                        }.disabled(pending)
                    }
                    if pending { ProgressView().controlSize(.small) }
                }.buttonStyle(.bordered).controlSize(.small)
                if let index = replyIndex {
                    HStack {
                        TextField("Reply", text: $reply).textFieldStyle(.roundedBorder).onSubmit { sendReply(index) }
                        Button("Send") { sendReply(index) }.disabled(reply.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || pending)
                    }
                }
            }
        }.padding(.vertical, 8)
    }
    private func sendReply(_ index: Int32) {
        guard !reply.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !pending else { return }
        onEvent(.reply(notification, index, reply))
        reply = ""; replyIndex = nil
    }
}

struct NotificationImage: View {
    let base64: String?
    let size: CGFloat
    var body: some View {
        Group {
            if let base64, let data = Data(base64Encoded: base64), let image = NSImage(data: data) {
                Image(nsImage: image).resizable().scaledToFit()
            } else { Image(systemName: "bell.fill").foregroundStyle(.secondary) }
        }.frame(width: size, height: size).clipShape(RoundedRectangle(cornerRadius: size / 4)).accessibilityHidden(true)
    }
}

func notificationID(_ notification: SyncedNotification) -> String {
    NativePresentation.shared.notificationId(notification: notification)
}

struct NotificationItem: Identifiable {
    let notification: SyncedNotification
    var id: String { notificationID(notification) }
}

private struct NotificationGroup: Identifiable {
    let notifications: [SyncedNotification]
    var id: String {
        guard let first = notifications.first else { return "empty" }
        return first.groupKey.isEmpty ? notificationID(first) : "\(first.deviceId):\(first.packageName):\(first.groupKey)"
    }
}
