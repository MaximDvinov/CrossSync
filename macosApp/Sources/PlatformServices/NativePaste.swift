import AppKit
@preconcurrency import ApplicationServices

@MainActor
enum NativePaste {
    static var isTrusted: Bool { AXIsProcessTrusted() }
    static func requestAccessibility() {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true] as CFDictionary
        _ = AXIsProcessTrustedWithOptions(options)
    }
    static func openAccessibilitySettings() {
        if let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility") { NSWorkspace.shared.open(url) }
    }
    static func paste(into application: NSRunningApplication?) {
        guard let application, !application.isTerminated else { return }
        application.activate(options: [])
        guard isTrusted else {
            requestAccessibility()
            return
        }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(180))
            guard NSWorkspace.shared.frontmostApplication?.processIdentifier == application.processIdentifier else { return }
            guard let source = CGEventSource(stateID: .hidSystemState),
                  let down = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: true),
                  let up = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: false) else { return }
            down.flags = .maskCommand; up.flags = .maskCommand
            down.post(tap: .cghidEventTap); up.post(tap: .cghidEventTap)
        }
    }
}
