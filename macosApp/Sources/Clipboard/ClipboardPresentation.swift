import SwiftUI
@preconcurrency import CrossSyncShared

/// Rendering only: source payloads stay in ViewModel state and are copied unchanged.
@MainActor
enum ClipboardPresentation {
    private static let richText = NSCache<NSString, NSAttributedString>()
    private static let images = NSCache<NSString, NSImage>()

    static func text(item: CopiedDataStable, preview: Bool) -> AttributedString {
        guard let formatted = item as? CopiedDataStable.FormattedText else {
            var result = AttributedString(NativePresentation.shared.text(item: item))
            result.font = .system(size: preview ? 12 : 14)
            return result
        }
        return text(source: formatted.text, plain: formatted.plainText, mimeType: formatted.mimeType, preview: preview)
    }

    static func text(source: String, plain: String, mimeType: String, preview: Bool) -> AttributedString {
        let key = "\(mimeType):\(source)" as NSString
        let attributed: NSAttributedString
        if let cached = richText.object(forKey: key) { attributed = cached }
        else {
            richText.countLimit = 200
            let type: NSAttributedString.DocumentType
            let data: Data?
            switch mimeType.lowercased() {
            case "text/html":
                type = .html
                data = localHTML(source).data(using: .utf8)
            case "text/rtf":
                type = .rtf
                data = source.hasPrefix("{\\rtf") ? source.data(using: .isoLatin1) : nil
            default:
                var result = AttributedString(plain)
                result.font = .system(size: preview ? 12 : 14)
                return result
            }
            if let data, let parsed = try? NSAttributedString(data: data,
                options: [.documentType: type, .characterEncoding: String.Encoding.utf8.rawValue], documentAttributes: nil),
               parsed.length > 0 {
                attributed = parsed
                richText.setObject(parsed, forKey: key, cost: data.count)
                richText.totalCostLimit = 8 * 1024 * 1024
            } else { attributed = NSAttributedString(string: plain) }
        }
        let normalized = NSMutableAttributedString(attributedString: attributed)
        let range = NSRange(location: 0, length: normalized.length)
        attributed.enumerateAttribute(.font, in: range) { value, run, _ in
            let original = value as? NSFont ?? .systemFont(ofSize: 14)
            let size = preview ? 12 : min(max(original.pointSize, 12), 28)
            normalized.addAttribute(.font, value: NSFont(descriptor: original.fontDescriptor, size: size) ?? .systemFont(ofSize: size), range: run)
        }
        // Neutral source text follows appearance; saturated source colors survive.
        attributed.enumerateAttribute(.foregroundColor, in: range) { value, run, _ in
            let color = value as? NSColor
            let background = attributed.attribute(.backgroundColor, at: run.location, effectiveRange: nil) as? NSColor
            let keepsBackground = !preview && background.map { !followsAppearance($0) } == true
            if keepsBackground && color == nil {
                normalized.addAttribute(.foregroundColor, value: NSColor.black, range: run)
            } else if !keepsBackground && (color == nil || followsAppearance(color!)) {
                normalized.addAttribute(.foregroundColor, value: NSColor.labelColor, range: run)
            }
        }
        attributed.enumerateAttribute(.backgroundColor, in: range) { value, run, _ in
            if let color = value as? NSColor, followsAppearance(color) {
                normalized.addAttribute(.backgroundColor, value: NSColor.textBackgroundColor, range: run)
            }
        }
        if preview {
            normalized.removeAttribute(.backgroundColor, range: range)
            normalized.removeAttribute(.attachment, range: range)
            let paragraph = NSMutableParagraphStyle()
            paragraph.paragraphSpacing = 0
            paragraph.lineSpacing = 0
            normalized.addAttribute(.paragraphStyle, value: paragraph, range: range)
            let string = normalized.string as NSString
            let first = string.rangeOfCharacter(from: .whitespacesAndNewlines.inverted)
            let last = string.rangeOfCharacter(from: .whitespacesAndNewlines.inverted, options: .backwards)
            if first.location != NSNotFound {
                let end = NSMaxRange(last)
                normalized.deleteCharacters(in: NSRange(location: end, length: normalized.length - end))
                normalized.deleteCharacters(in: NSRange(location: 0, length: first.location))
            }
        }
        // SwiftUI doesn't bridge AppKit font/color attributes automatically.
        var result = AttributedString()
        normalized.enumerateAttributes(in: NSRange(location: 0, length: normalized.length)) { attributes, runRange, _ in
            var run = AttributedString((normalized.string as NSString).substring(with: runRange))
            if let font = attributes[.font] as? NSFont { run.font = Font(font) }
            if let color = attributes[.foregroundColor] as? NSColor { run.foregroundColor = Color(nsColor: color) }
            if let color = attributes[.backgroundColor] as? NSColor { run.backgroundColor = Color(nsColor: color) }
            if let value = attributes[.underlineStyle] as? NSNumber, value.intValue != 0 {
                run.underlineStyle = .init(pattern: .solid)
            }
            if let value = attributes[.strikethroughStyle] as? NSNumber, value.intValue != 0 {
                run.strikethroughStyle = .init(pattern: .solid)
            }
            if let url = attributes[.link] as? URL { run.link = url }
            else if let url = attributes[.link] as? String { run.link = URL(string: url) }
            if let offset = attributes[.baselineOffset] as? NSNumber { run.baselineOffset = offset.doubleValue }
            result.append(run)
        }
        return result
    }

    static func image(path: String) -> NSImage? {
        let key = "file:\(path)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        guard let image = NSImage(contentsOfFile: path) else { return nil }
        images.countLimit = 100
        images.setObject(image, forKey: key)
        return image
    }

    static func applicationIcon(_ application: Application?) -> NSImage? {
        guard let application else { return nil }
        let key = "app:\(application.id):\(application.icon ?? application.path)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let image: NSImage?
        if let icon = application.icon, let data = Data(base64Encoded: icon), let decoded = NSImage(data: data) {
            image = decoded
        } else if let icon = application.icon, let local = NSImage(contentsOfFile: icon) {
            image = local
        } else if FileManager.default.fileExists(atPath: application.path) {
            image = NSWorkspace.shared.icon(forFile: application.path)
        } else { image = nil }
        if let image { images.countLimit = 100; images.setObject(image, forKey: key) }
        return image
    }

    private static func localHTML(_ source: String) -> String {
        // A clipboard preview imports styles and text without fetching embedded resources.
        var result = source.replacingOccurrences(of: #"(?is)<(script|iframe|object|embed)\b[^>]*>.*?</\1\s*>"#, with: "", options: .regularExpression)
        result = result.replacingOccurrences(of: #"(?is)<(img|link|base|source|embed)\b[^>]*>"#, with: "", options: .regularExpression)
        result = result.replacingOccurrences(of: #"(?is)@import\s+[^;]+;|url\s*\([^)]*\)"#, with: "", options: .regularExpression)
        return result
    }

    private static func followsAppearance(_ color: NSColor) -> Bool {
        guard let rgb = color.usingColorSpace(.deviceRGB) else { return false }
        let components = [rgb.redComponent, rgb.greenComponent, rgb.blueComponent]
        let darkest = components.min() ?? 0
        let brightest = components.max() ?? 0
        return brightest - darkest < 0.15 && (brightest < 0.45 || darkest > 0.85)
    }
}
