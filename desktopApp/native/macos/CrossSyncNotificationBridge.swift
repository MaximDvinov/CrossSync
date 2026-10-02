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
        guard let rect = screenRect() else { return nil }
        return NSPoint(x: rect.midX, y: rect.midY)
    }

    func screenRect() -> NSRect? {
        if Thread.isMainThread {
            return screenRectOnMain()
        }
        return DispatchQueue.main.sync { screenRectOnMain() }
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

    private func screenRectOnMain() -> NSRect? {
        guard
            let button = statusItem?.button,
            let window = button.window
        else {
            return nil
        }

        let buttonRect = button.convert(button.bounds, to: nil)
        return window.convertToScreen(buttonRect)
    }

    private func performOnMain(_ action: @escaping () -> Void) {
        if Thread.isMainThread {
            action()
        } else {
            DispatchQueue.main.async(execute: action)
        }
    }
}

private final class CrossSyncLiveUpdatePopupMonitor {
    private weak var popupWindow: NSWindow?
    private var onDismiss: CrossSyncLiveUpdateAction?
    private var localMonitor: Any?
    private var globalMonitor: Any?
    private var activationObserver: NSObjectProtocol?

    func observe(window: NSWindow?, onDismiss: CrossSyncLiveUpdateAction?) {
        stop()
        guard let window, let onDismiss else { return }
        popupWindow = window
        self.onDismiss = onDismiss

        let clicks: NSEvent.EventTypeMask = [.leftMouseDown, .rightMouseDown, .otherMouseDown]
        localMonitor = NSEvent.addLocalMonitorForEvents(matching: clicks) { [weak self] event in
            self?.handleClick()
            return event
        }
        globalMonitor = NSEvent.addGlobalMonitorForEvents(matching: clicks) { [weak self] _ in
            self?.handleClick()
        }
        activationObserver = NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.didActivateApplicationNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard
                let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication,
                app.processIdentifier != ProcessInfo.processInfo.processIdentifier
            else { return }
            self?.dismiss()
        }
    }

    private func handleClick() {
        guard let popupWindow else { return }
        let point = NSEvent.mouseLocation
        if popupWindow.frame.contains(point) { return }
        // The status-button action owns toggling; dismissing on mouse-down would reopen on mouse-up.
        if liveUpdateStatusItemController.screenRect()?.contains(point) == true { return }
        dismiss()
    }

    private func dismiss() {
        let callback = onDismiss
        stop()
        callback?()
    }

    private func stop() {
        if let localMonitor { NSEvent.removeMonitor(localMonitor) }
        if let globalMonitor { NSEvent.removeMonitor(globalMonitor) }
        if let activationObserver {
            NSWorkspace.shared.notificationCenter.removeObserver(activationObserver)
        }
        localMonitor = nil
        globalMonitor = nil
        activationObserver = nil
        popupWindow = nil
        onDismiss = nil
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
private let liveUpdatePopupMonitor = CrossSyncLiveUpdatePopupMonitor()

@_cdecl("crosssync_observe_live_update_popup")
public func crosssync_observe_live_update_popup(
    _ windowPointer: UnsafeMutableRawPointer?,
    _ onDismiss: CrossSyncLiveUpdateAction?
) {
    let observe = {
        let window = windowPointer.map { Unmanaged<NSWindow>.fromOpaque($0).takeUnretainedValue() }
        liveUpdatePopupMonitor.observe(window: window, onDismiss: onDismiss)
    }
    // Disposal must finish before Compose releases its NSWindow or the JNA callback.
    if Thread.isMainThread {
        observe()
    } else {
        DispatchQueue.main.sync(execute: observe)
    }
}

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
