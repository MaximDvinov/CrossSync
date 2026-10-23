import AppKit
import SwiftUI
@preconcurrency import CrossSyncShared

@main
struct ClipboardPresentationTests {
    @MainActor static func main() throws {
        _ = NSApplication.shared
        let html = ClipboardPresentation.text(source: "<p><b>Bold</b> <i>Italic</i> <u>Underline</u> <a href='https://example.com'>Link</a></p>",
            plain: "Bold Italic Underline Link", mimeType: "text/html", preview: true)
        precondition(String(html.characters).contains("Bold Italic Underline Link"))
        let bold = html.runs.first { String(html[$0.range].characters) == "Bold" }!
        let italic = html.runs.first { String(html[$0.range].characters) == "Italic" }!
        let underline = html.runs.first { String(html[$0.range].characters) == "Underline" }!
        let link = html.runs.first { String(html[$0.range].characters) == "Link" }!
        let document = try NSAttributedString(data: Data("<b>Bold</b><i>Italic</i>".utf8), options: [.documentType: NSAttributedString.DocumentType.html], documentAttributes: nil)
        let boldFont = document.attribute(.font, at: 0, effectiveRange: nil) as! NSFont
        let italicFont = document.attribute(.font, at: 4, effectiveRange: nil) as! NSFont
        precondition(bold.font == Font(NSFont(descriptor: boldFont.fontDescriptor, size: 12)!))
        precondition(italic.font == Font(NSFont(descriptor: italicFont.fontDescriptor, size: 12)!))
        precondition(underline.underlineStyle != nil)
        precondition(link.link?.host == "example.com" && link.link?.scheme == "https")
        let neutral = ClipboardPresentation.text(source: "<p style='color:#202328'>Readable</p>", plain: "Readable", mimeType: "text/html", preview: true)
        precondition(String(neutral.characters) == "Readable")
        precondition(neutral.runs.first?.foregroundColor == Color(nsColor: .labelColor))
        let colored = ClipboardPresentation.text(source: "<span style='color:#ff0000'>Red</span>", plain: "Red", mimeType: "text/html", preview: true)
        precondition(colored.runs.first?.foregroundColor == Color(nsColor: .red))
        let background = ClipboardPresentation.text(source: "<span style='color:#202328;background-color:#ffffff'>Readable</span>", plain: "Readable", mimeType: "text/html", preview: false)
        precondition(background.runs.first?.foregroundColor == Color(nsColor: .labelColor))
        precondition(background.runs.first?.backgroundColor == Color(nsColor: .textBackgroundColor))
        let highlight = ClipboardPresentation.text(source: "<span style='color:#000000;background-color:#ffff00'>Highlight</span>", plain: "Highlight", mimeType: "text/html", preview: false)
        let highlightedDocument = try NSAttributedString(data: Data("<span style='color:#000000;background-color:#ffff00'>Highlight</span>".utf8), options: [.documentType: NSAttributedString.DocumentType.html], documentAttributes: nil)
        let highlightedColor = highlightedDocument.attribute(.foregroundColor, at: 0, effectiveRange: nil) as? NSColor ?? .black
        precondition(highlight.runs.first?.foregroundColor == Color(nsColor: highlightedColor))
        let rtf = ClipboardPresentation.text(source: "{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Helvetica;}}\\f0\\fs28 plain \\b bold\\b0  \\i italic\\i0}",
            plain: "plain bold italic", mimeType: "text/rtf", preview: true)
        precondition(String(rtf.characters) == "plain bold italic")
        let rtfBold = rtf.runs.first { String(rtf[$0.range].characters).contains("bold") }!
        let rtfPlain = rtf.runs.first { String(rtf[$0.range].characters).contains("plain") }!
        precondition(rtfBold.font != rtfPlain.font)
        let fallback = ClipboardPresentation.text(source: "invalid rtf", plain: "Fallback text", mimeType: "text/rtf", preview: true)
        precondition(String(fallback.characters) == "Fallback text")
        let application = Application(id: "test", name: "Fixture", icon: NSImage(systemSymbolName: "app", accessibilityDescription: nil)!.tiffRepresentation!.base64EncodedString(), path: "/missing")
        precondition(ClipboardPresentation.applicationIcon(application) != nil)
        precondition(ClipboardPresentation.applicationIcon(Application(id: "missing", name: "Missing", icon: nil, path: "/missing")) == nil)
        print("PASS: HTML/RTF styling, compact font sizes, links, malformed payload fallback, application icons")
    }
}
