import AppKit
import CryptoKit
import ServiceManagement
@preconcurrency import CrossSyncShared

// Kotlin invokes clipboard, application and login-item operations on Dispatchers.Main.
@MainActor
final class MacDesktopServices: NSObject, @preconcurrency DesktopServices {
    private let pasteboard: NSPasteboard
    private let imageDirectory: URL
    private let sourceApplication: () -> String?
    init(pasteboard: NSPasteboard = .general, imageDirectory: URL = MacDesktopServices.dataDirectory.appendingPathComponent("Images", isDirectory: true), sourceApplication: @escaping () -> String? = { NSWorkspace.shared.frontmostApplication?.bundleIdentifier }) {
        self.pasteboard = pasteboard
        self.imageDirectory = imageDirectory
        self.sourceApplication = sourceApplication
        super.init()
    }
    func clipboardRevision() -> Int32 {
        Int32(truncatingIfNeeded: pasteboard.changeCount)
    }
    static let dataDirectory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("CrossSyncNative", isDirectory: true)

    func readClipboard() -> NativeClipboardContent? {
        readClipboardOnMain()
    }

    @MainActor private func readClipboardOnMain() -> NativeClipboardContent? {
            let board = pasteboard
            let source = sourceApplication()
            let plain = board.string(forType: .string) ?? ""
            if let urls = board.readObjects(forClasses: [NSURL.self], options: [.urlReadingFileURLsOnly: true]) as? [URL], !urls.isEmpty {
                return content("files", paths: urls.map(\.path), source: source)
            }
            if let data = board.data(forType: .png) ?? board.data(forType: .tiff), let image = NSImage(data: data),
               let tiff = image.tiffRepresentation, let representation = NSBitmapImageRep(data: tiff),
               let png = representation.representation(using: .png, properties: [:]) {
                let directory = imageDirectory
                let name = SHA256.hash(data: png).map { String(format: "%02x", $0) }.joined() + ".png"
                let url = directory.appendingPathComponent(name)
                do {
                    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
                    if !FileManager.default.fileExists(atPath: url.path) { try png.write(to: url, options: .atomic) }
                    return content("image", paths: [url.path], source: source)
                } catch { return nil }
            }
            if let html = board.string(forType: .html) {
                return content("html", text: html, plain: plain.isEmpty ? plainText(Data(html.utf8), .html) : plain, source: source)
            }
            if let rtf = board.data(forType: .rtf), let text = String(data: rtf, encoding: .isoLatin1) {
                return content("rtf", text: text, plain: plain.isEmpty ? plainText(rtf, .rtf) : plain, source: source)
            }
            return plain.isEmpty ? nil : content("text", text: plain, plain: plain, source: source)
    }

    func writeClipboard(content: NativeClipboardContent) -> String? {
            let board = pasteboard
            switch content.kind {
            case "files":
                let urls = content.paths.map { NSURL(fileURLWithPath: $0) }
                guard !urls.isEmpty, urls.allSatisfy({ FileManager.default.fileExists(atPath: $0.path!) }) else { return "The original file is no longer available." }
                board.clearContents(); board.writeObjects(urls)
            case "image":
                guard let path = content.paths.first, let image = NSImage(contentsOfFile: path) else { return "The saved image is no longer available." }
                board.clearContents(); board.writeObjects([image])
            default:
                board.clearContents()
                board.setString(content.plainText, forType: .string)
                if content.kind == "html" { board.setString(content.text, forType: .html) }
                if content.kind == "rtf", let data = content.text.data(using: .isoLatin1) { board.setData(data, forType: .rtf) }
            }
            return nil
    }

    func installedApplications() -> [NativeApplication] {
        applicationsOnMain()
    }

    @MainActor private func applicationsOnMain() -> [NativeApplication] {
            let directories = ["/Applications", "/System/Applications", NSHomeDirectory() + "/Applications"]
            var applications: [String: NativeApplication] = [:]
            for directory in directories {
                guard let enumerator = FileManager.default.enumerator(at: URL(fileURLWithPath: directory), includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles]) else { continue }
                for case let url as URL in enumerator where url.pathExtension == "app" {
                    enumerator.skipDescendants()
                    guard let bundle = Bundle(url: url), let id = bundle.bundleIdentifier else { continue }
                    let icon = NSWorkspace.shared.icon(forFile: url.path)
                    let png = thumbnail(icon)
                    applications[id] = NativeApplication(id: id, name: bundle.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? url.deletingPathExtension().lastPathComponent, icon: png?.base64EncodedString(), path: url.path)
                }
            }
            applications["com.apple.finder"] = NativeApplication(id: "com.apple.finder", name: "Finder", icon: nil, path: "/System/Library/CoreServices/Finder.app")
            return applications.values.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
    }

    nonisolated func lanAddresses() -> [String] {
        var head: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&head) == 0, let first = head else { return [] }
        defer { freeifaddrs(head) }
        var result: [(String, String)] = []
        var cursor: UnsafeMutablePointer<ifaddrs>? = first
        while let current = cursor {
            defer { cursor = current.pointee.ifa_next }
            let interface = current.pointee
            guard let address = interface.ifa_addr, address.pointee.sa_family == UInt8(AF_INET),
                  interface.ifa_flags & UInt32(IFF_UP) != 0, interface.ifa_flags & UInt32(IFF_LOOPBACK | IFF_POINTOPOINT) == 0 else { continue }
            let name = String(cString: interface.ifa_name)
            guard !["utun", "awdl", "llw", "bridge", "vmnet", "tun", "tap"].contains(where: name.hasPrefix) else { continue }
            var buffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            guard getnameinfo(address, socklen_t(address.pointee.sa_len), &buffer, socklen_t(buffer.count), nil, 0, NI_NUMERICHOST) == 0 else { continue }
            let ip = String(decoding: buffer.prefix { $0 != 0 }.map { UInt8(bitPattern: $0) }, as: UTF8.self)
            let bytes = ip.split(separator: ".").compactMap { Int($0) }
            guard bytes.count == 4, bytes[0] == 10 || (bytes[0] == 192 && bytes[1] == 168) || (bytes[0] == 172 && (16...31).contains(bytes[1])) else { continue }
            result.append((name, ip))
        }
        return Array(Set(result.sorted { $0.0 < $1.0 }.map(\.1))).sorted()
    }
    nonisolated func deviceName() -> String { Host.current().localizedName ?? ProcessInfo.processInfo.hostName }
    func autoStartEnabled() -> Bool { SMAppService.mainApp.status == .enabled }
    func setAutoStart(enabled: Bool) -> String? {
        do {
            if enabled { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
            if enabled && SMAppService.mainApp.status == .requiresApproval {
                return "Allow CrossSync in System Settings → General → Login Items."
            }
            return nil
        } catch { return error.localizedDescription }
    }
    private func content(_ kind: String, text: String = "", plain: String = "", paths: [String] = [], source: String?) -> NativeClipboardContent {
        NativeClipboardContent(kind: kind, text: text, plainText: plain, paths: paths, applicationId: source)
    }
    private func plainText(_ data: Data, _ type: NSAttributedString.DocumentType) -> String {
        (try? NSAttributedString(data: data, options: [.documentType: type], documentAttributes: nil))?.string ?? ""
    }
    private func thumbnail(_ image: NSImage) -> Data? {
        guard let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 48, pixelsHigh: 48,
                                          bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                                          isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0),
              let context = NSGraphicsContext(bitmapImageRep: bitmap) else { return nil }
        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = context
        image.draw(in: NSRect(x: 0, y: 0, width: 48, height: 48), from: .zero, operation: .copy, fraction: 1)
        NSGraphicsContext.restoreGraphicsState()
        return bitmap.representation(using: .png, properties: [:])
    }
}
