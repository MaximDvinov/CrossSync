import Foundation
import AppKit
import UserNotifications

public typealias CrossSyncLiveUpdateAction = @convention(c) () -> Void

private final class CrossSyncLiveUpdateStatusItemController: NSObject {
    private var statusItem: NSStatusItem?
    private var action: CrossSyncLiveUpdateAction?

    func update(title: String, iconBase64: String?, action: CrossSyncLiveUpdateAction?) {
        performOnMain { [weak self] in
            guard let self else { return }

            if title.isEmpty {
                if let statusItem {
                    NSStatusBar.system.removeStatusItem(statusItem)
                    self.statusItem = nil
                }
                self.action = nil
                return
            }

            self.action = action
            let statusItem = self.statusItem ?? NSStatusBar.system.statusItem(
                withLength: NSStatusItem.variableLength,
            )
            self.statusItem = statusItem
            if let button = statusItem.button {
                button.image = image(from: iconBase64)
                button.title = title
                button.imagePosition = .imageLeading
                button.target = self
                button.action = #selector(handleClick)
                button.setAccessibilityLabel("CrossSync Live Update")
            }
        }
    }

    func screenPoint() -> NSPoint? {
        if Thread.isMainThread {
            return screenPointOnMain()
        }
        return DispatchQueue.main.sync { screenPointOnMain() }
    }

    @objc private func handleClick() {
        action?()
    }

    private func image(from base64: String?) -> NSImage? {
        guard
            let base64,
            let data = Data(base64Encoded: base64),
            let image = NSImage(data: data)
        else {
            return nil
        }

        image.size = NSSize(width: 18, height: 18)
        return image
    }

    private func screenPointOnMain() -> NSPoint? {
        guard
            let button = statusItem?.button,
            let window = button.window
        else {
            return nil
        }

        let buttonRect = button.convert(button.bounds, to: nil)
        let screenRect = window.convertToScreen(buttonRect)
        return NSPoint(x: screenRect.midX, y: screenRect.midY)
    }

    private func performOnMain(_ action: @escaping () -> Void) {
        if Thread.isMainThread {
            action()
        } else {
            DispatchQueue.main.async(execute: action)
        }
    }
}

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
private let liveUpdateStatusItemController = CrossSyncLiveUpdateStatusItemController()

@_cdecl("crosssync_set_live_update_status_item")
public func crosssync_set_live_update_status_item(
    _ titlePointer: UnsafePointer<CChar>?,
    _ iconBase64Pointer: UnsafePointer<CChar>?,
    _ action: CrossSyncLiveUpdateAction?
) {
    let title = titlePointer.map { String(cString: $0) } ?? ""
    let iconBase64 = iconBase64Pointer.map { String(cString: $0) }
    liveUpdateStatusItemController.update(title: title, iconBase64: iconBase64, action: action)
}

@_cdecl("crosssync_get_live_update_status_item_position")
public func crosssync_get_live_update_status_item_position(
    _ coordinates: UnsafeMutablePointer<Int32>?
) -> Int32 {
    guard let coordinates, let point = liveUpdateStatusItemController.screenPoint() else {
        return 0
    }

    coordinates[0] = Int32(point.x)
    coordinates[1] = Int32(point.y)
    return 1
}

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
