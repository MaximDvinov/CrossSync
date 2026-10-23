import Foundation
import UserNotifications
@preconcurrency import CrossSyncShared

@MainActor
final class SystemNotifications: NSObject, @preconcurrency SystemNotificationSink, UNUserNotificationCenterDelegate {
    @MainActor var onAction: ((String, Int32?, String?) -> Void)?
    @MainActor private var categories: [String: UNNotificationCategory] = [:]
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }
    func requestAuthorization(completed: @escaping @MainActor @Sendable (String) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            let message = error?.localizedDescription ?? (granted ? "System notifications are enabled." : "Notifications are disabled. Allow CrossSync in System Settings → Notifications.")
            Task { @MainActor in completed(message) }
        }
    }
    func publish(notification: SyncedNotification) {
        publishOnMain(notification)
    }
    @MainActor private func publishOnMain(_ notification: SyncedNotification) {
        let content = UNMutableNotificationContent()
        content.title = notification.hasPreview && !notification.title.isEmpty ? notification.title : notification.appName
        content.subtitle = notification.appName
        content.body = notification.hasPreview ? notification.body : "Content hidden"
        content.sound = .default
        let id = NativePresentation.shared.notificationId(notification: notification)
        content.userInfo = ["notificationID": id]
        let categoryID = "crosssync." + id
        content.categoryIdentifier = categoryID
        let actions: [UNNotificationAction] = notification.actions.prefix(4).map { action in
            let identifier = "action.\(action.index)"
            if action.supportsReply {
                return UNTextInputNotificationAction(identifier: identifier, title: action.title, options: [], textInputButtonTitle: "Send", textInputPlaceholder: "Reply")
            }
            return UNNotificationAction(identifier: identifier, title: action.title, options: [])
        }
        let category = UNNotificationCategory(identifier: categoryID, actions: actions, intentIdentifiers: [], options: [])
        let center = UNUserNotificationCenter.current()
        categories[categoryID] = category
        center.setNotificationCategories(Set(categories.values))
        if notification.hasPreview, let base64 = notification.media?.image, let data = Data(base64Encoded: base64) {
            let directory = MacDesktopServices.dataDirectory.appendingPathComponent("NotificationImages", isDirectory: true)
            let path = directory.appendingPathComponent(UUID().uuidString + ".png")
            if let _ = try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true),
               let _ = try? data.write(to: path), let attachment = try? UNNotificationAttachment(identifier: "image", url: path) {
                content.attachments = [attachment]
            }
        }
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: nil)) { _ in }
    }
    func withdraw(id: String) {
            let center = UNUserNotificationCenter.current()
            center.removeDeliveredNotifications(withIdentifiers: [id])
            center.removePendingNotificationRequests(withIdentifiers: [id])
            categories.removeValue(forKey: "crosssync." + id)
            center.setNotificationCategories(Set(categories.values))
    }
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completion: @escaping (UNNotificationPresentationOptions) -> Void) {
        completion([.banner, .sound, .list])
    }
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completion: @escaping () -> Void) {
        let id = response.notification.request.content.userInfo["notificationID"] as? String ?? ""
        let action = response.actionIdentifier
        let index = action.hasPrefix("action.") ? Int32(action.dropFirst(7)) : nil
        let reply = (response as? UNTextInputNotificationResponse)?.userText
        Task { @MainActor in self.onAction?(id, index, reply) }
        completion()
    }
}
