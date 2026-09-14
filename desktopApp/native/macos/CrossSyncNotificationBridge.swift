import Foundation
import UserNotifications

private final class CrossSyncNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if #available(macOS 11.0, *) {
            completionHandler([.banner, .sound])
        } else {
            completionHandler([.alert, .sound])
        }
    }
}

private let notificationDelegate = CrossSyncNotificationDelegate()

@_cdecl("crosssync_publish_notification")
public func crosssync_publish_notification(
    _ titlePointer: UnsafePointer<CChar>?,
    _ subtitlePointer: UnsafePointer<CChar>?,
    _ bodyPointer: UnsafePointer<CChar>?
) {
    let title = titlePointer.map { String(cString: $0) } ?? "CrossSync"
    let subtitle = subtitlePointer.map { String(cString: $0) } ?? ""
    let body = bodyPointer.map { String(cString: $0) } ?? ""

    DispatchQueue.main.async {
        let notificationCenter = UNUserNotificationCenter.current()
        notificationCenter.delegate = notificationDelegate

        let content = UNMutableNotificationContent()
        content.title = title
        content.subtitle = subtitle
        content.body = body
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        notificationCenter.requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            guard granted else {
                if let error {
                    NSLog("CrossSync: notification authorization failed: %@", error.localizedDescription)
                }
                return
            }

            notificationCenter.add(request) { error in
                if let error {
                    NSLog("CrossSync: notification delivery failed: %@", error.localizedDescription)
                }
            }
        }
    }
}
