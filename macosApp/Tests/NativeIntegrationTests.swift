import AppKit
import CryptoKit
import CrossSyncShared

private struct TestFailure: Error { let message: String }

@MainActor private func expect(_ value: @autoclosure () -> Bool, _ message: String) throws {
    guard value() else { throw TestFailure(message: message) }
}

@MainActor private func eventually(_ message: String, _ predicate: () -> Bool) async throws {
    for _ in 0..<200 {
        if predicate() { return }
        try await Task.sleep(for: .milliseconds(50))
    }
    throw TestFailure(message: message)
}

@MainActor private final class TestNotifications: NSObject, @preconcurrency SystemNotificationSink {
    var delivered: [String] = []
    var withdrawn: [String] = []
    func publish(notification: SyncedNotification) { delivered.append(notification.notificationKey) }
    func withdraw(id: String) { withdrawn.append(id) }
}

@main private struct NativeIntegrationTests {
    @MainActor private static var port: Int32 = 0
    @MainActor static func main() {
        setvbuf(stdout, nil, _IONBF, 0)
        _ = NSApplication.shared
        NSApp.setActivationPolicy(.prohibited)
        Task { @MainActor in
            do { try await runTests(); exit(0) }
            catch { print("FAIL: \(error)"); exit(1) }
        }
        // Kotlin/Native's Main dispatcher uses the main CFRunLoop.
        NSApp.run()
    }
    @MainActor private static func runTests() async throws {
        _ = NSApplication.shared
        let crypto = CryptoKitOperations()
        let key = Data(repeating: 0, count: 32).base64EncodedString()
        let tag = "530f8afbc74536b9a963b4f1c4cb738b"
        let fixture = Data(repeating: 0, count: 12) + Data(stride(from: 0, to: tag.count, by: 2).map { offset in
            let start = tag.index(tag.startIndex, offsetBy: offset)
            return UInt8(tag[start..<tag.index(start, offsetBy: 2)], radix: 16)!
        })
        try expect(crypto.open(cipherText: fixture.base64EncodedString(), base64Key: key) == "", "AES-256-GCM nonce/ciphertext/tag compatibility")
        let ciphertext = crypto.seal(plainText: "Привет 👋", base64Key: key)
        try expect(crypto.open(cipherText: ciphertext, base64Key: key) == "Привет 👋", "UTF-8 round trip")
        var tampered = Data(base64Encoded: ciphertext)!
        tampered[tampered.count - 1] ^= 1
        try expect(crypto.open(cipherText: tampered.base64EncodedString(), base64Key: key) == nil, "Tampered payload rejected")

        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("CrossSyncTests-" + UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let suite = "com.cross.sync.tests." + UUID().uuidString
        let board = NSPasteboard(name: NSPasteboard.Name(suite))
        let desktop = MacDesktopServices(pasteboard: board, imageDirectory: directory.appendingPathComponent("Images"), sourceApplication: { "fixture.application" })
        let notifications = TestNotifications()
        var passed = false
        defer {
            board.releaseGlobally()
            UserDefaults(suiteName: suite)?.removePersistentDomain(forName: suite)
            if passed { try? FileManager.default.removeItem(at: directory) }
            else { print("Test database retained at \(directory.path)") }
        }

        for (kind, text) in [("text", "plain"), ("html", "<b>formatted</b>"), ("rtf", "{\\rtf1\\ansi formatted}")] {
            let item = NativeClipboardContent(kind: kind, text: text, plainText: "formatted", paths: [], applicationId: nil)
            try expect(desktop.writeClipboard(content: item) == nil, "Write \(kind)")
            try expect(desktop.readClipboard()?.kind == kind, "Read \(kind)")
        }
        let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 2, pixelsHigh: 2, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
        let imageData = bitmap.representation(using: .png, properties: [:])!
        board.clearContents(); board.setData(imageData, forType: .png)
        let image = desktop.readClipboard()!
        try expect(image.kind == "image" && FileManager.default.fileExists(atPath: image.paths[0]), "Image is stored locally")
        try expect(desktop.writeClipboard(content: image) == nil && desktop.readClipboard()?.kind == "image", "Image round trip")
        let file = directory.appendingPathComponent("file.txt")
        try Data("fixture".utf8).write(to: file)
        let item = NativeClipboardContent(kind: "files", text: "", plainText: "", paths: [file.path], applicationId: nil)
        try expect(desktop.writeClipboard(content: item) == nil, "Write files")
        try expect(desktop.readClipboard()?.paths == [file.path], "Read file URLs")
        let missing = NativeClipboardContent(kind: "files", text: "", plainText: "", paths: [directory.appendingPathComponent("missing").path], applicationId: nil)
        let revision = desktop.clipboardRevision()
        try expect(desktop.writeClipboard(content: missing) != nil && desktop.clipboardRevision() == revision, "Missing file preserves pasteboard")

        // All data and preferences are isolated from the user's real application.
        let runtime = MacRuntime(desktop: desktop, crypto: crypto, system: notifications, dataDirectory: directory.path, settingsSuite: suite, listenPort: port)
        let clipboard = runtime.clipboardScreen()
        let otherWindow = runtime.clipboardScreen()
        let settings = runtime.settingsScreen()
        let notificationScreen = runtime.notificationsScreen()
        settings.pair()
        try await eventually("Initial server selects a free port") { settings.qrCode != nil }
        let initialQR = try JSONSerialization.jsonObject(with: Data(settings.qrCode!.utf8)) as! [String: Any]
        port = (initialQR["port"] as! NSNumber).int32Value
        try expect(port > 0, "QR contains the actual listening port")
        try await eventually("Initial local file history") { !clipboard.current.copiedDataList.isEmpty }
        let count = clipboard.current.copiedDataList.count
        try await Task.sleep(for: .milliseconds(1700))
        try expect(clipboard.current.copiedDataList.count == count, "Multiple windows do not duplicate clipboard capture")
        try expect(runtime.current.serverError == nil, "CIO server starts once")
        let conflict = MacRuntime(desktop: desktop, crypto: crypto, system: TestNotifications(), dataDirectory: directory.appendingPathComponent("Conflict").path, settingsSuite: suite + ".conflict", listenPort: port)
        let conflictSettings = conflict.settingsScreen()
        conflictSettings.pair()
        do {
            try await eventually("Occupied port selects a free port") { conflictSettings.qrCode != nil }
        } catch {
            let failure = conflictSettings.current.pairingState as? PairingStateError
            throw TestFailure(message: "Occupied port selects a free port: " + (failure?.throwable.message ?? conflict.current.serverError ?? "startup did not complete"))
        }
        let conflictQR = try JSONSerialization.jsonObject(with: Data(conflictSettings.qrCode!.utf8)) as! [String: Any]
        let conflictPort = (conflictQR["port"] as! NSNumber).int32Value
        try expect(conflictPort > 0 && conflictPort != port && conflict.current.serverError == nil, "Second server starts on another port")
        let (_, conflictResponse) = try await URLSession.shared.data(from: URL(string: "http://127.0.0.1:\(conflictPort)/")!)
        try expect((conflictResponse as! HTTPURLResponse).statusCode == 404, "Fallback QR port reaches the CIO server")
        conflictSettings.close()
        await withCheckedContinuation { continuation in conflict.close { continuation.resume() } }
        UserDefaults(suiteName: suite + ".conflict")?.removePersistentDomain(forName: suite + ".conflict")
        try expect(runtime.current.serverError == nil, "The original server survives a port conflict")
        settings.addCategory(name: "Fixture")
        try await eventually("Category saved") { settings.current.categories.contains { $0.name == "Fixture" } }
        let category = settings.current.categories.first { $0.name == "Fixture" }!
        clipboard.moveToCategory(id: clipboard.current.copiedDataList[0].id, categoryId: category.id)
        clipboard.selectCategory(id: category.id)
        try await eventually("Category binding") { clipboard.current.copiedDataList.count == 1 }
        settings.renameCategory(id: category.id, name: "Renamed fixture")
        try await eventually("Selected category is renamed") { clipboard.current.selectedCategory?.name == "Renamed fixture" }
        settings.addCategory(name: "Move destination")
        try await eventually("Second category saved") { settings.current.categories.contains { $0.name == "Move destination" } }
        let destination = settings.current.categories.first { $0.name == "Move destination" }!
        let movedID = clipboard.current.copiedDataList[0].id
        clipboard.moveToCategory(id: movedID, categoryId: destination.id)
        try await eventually("Move removes source binding") { clipboard.current.copiedDataList.isEmpty }
        clipboard.selectCategory(id: destination.id)
        try await eventually("Move creates destination binding") { clipboard.current.copiedDataList.first?.id == movedID }
        settings.deleteCategory(id: destination.id)
        try await eventually("Deleting selected category resets selection") { clipboard.current.selectedCategory == nil }
        clipboard.selectCategory(id: -2)
        try await eventually("Deleted category item becomes uncategorized") { clipboard.current.copiedDataList.contains { $0.id == movedID } }
        clipboard.selectCategory(id: category.id)
        otherWindow.moveToCategory(id: movedID, categoryId: category.id)
        try await eventually("Category gets its item") { clipboard.current.copiedDataList.contains { $0.id == movedID } }
        clipboard.clear()
        try await eventually("Clear selected category") { clipboard.current.copiedDataList.isEmpty && !otherWindow.current.copiedDataList.contains { $0.id == movedID } }
        settings.setQuickSize(size: 50)
        settings.setRetention(days: 15)
        let otherSettings = runtime.settingsScreen()
        settings.setTheme(value: "DARK")
        try await eventually("Preferences update in another window") { otherSettings.current.themeMode.name == "DARK" }
        otherSettings.close()
        settings.exclude(id: "fixture.application")
        try await eventually("Excluded app preference") { settings.current.excludedApplicationIds.contains("fixture.application") }
        try expect(desktop.writeClipboard(content: NativeClipboardContent(kind: "text", text: "Ignored app fixture", plainText: "Ignored app fixture", paths: [], applicationId: nil)) == nil, "Excluded app clipboard write")
        try await Task.sleep(for: .milliseconds(1700))
        try expect(!otherWindow.current.copiedDataList.contains { NativePresentation.shared.text(item: $0) == "Ignored app fixture" }, "Excluded application is not captured")
        settings.include(id: "fixture.application")
        try await eventually("Included app preference") { !settings.current.excludedApplicationIds.contains("fixture.application") }
        for (kind, text) in [("text", "local history"), ("html", "<b>local history</b>"), ("rtf", "{\\rtf1\\ansi local history}")] {
            let content = NativeClipboardContent(kind: kind, text: text, plainText: "local history", paths: [], applicationId: nil)
            try expect(desktop.writeClipboard(content: content) == nil, "Capture \(kind)")
            try await eventually("Local history retains \(kind)") { otherWindow.current.copiedDataList.contains { NativePresentation.shared.kind(item: $0) == kind } }
        }
        try expect(desktop.writeClipboard(content: image) == nil, "Capture image")
        try await eventually("Local image history") { otherWindow.current.copiedDataList.contains { NativePresentation.shared.kind(item: $0) == "image" } }
        let imageEntry = otherWindow.current.copiedDataList.first { NativePresentation.shared.kind(item: $0) == "image" }!
        let copyError: String? = await withCheckedContinuation { continuation in otherWindow.doCopy(id: imageEntry.id) { continuation.resume(returning: $0) } }
        try expect(copyError == nil && desktop.readClipboard()?.kind == "image", "Copy local image from history")

        settings.pair()
        try await eventually("QR generated") { settings.qrCode != nil }
        let qr = try JSONSerialization.jsonObject(with: Data(settings.qrCode!.utf8)) as! [String: Any]
        let secret = qr["secretKey"] as! String
        let paired = try await post("/pair", ["id": "fixture-phone", "name": "Android fixture", "pairingKey": qr["pairingKey"]!])
        try expect(paired.status == 200, "Pair protocol")
        let tokens = try JSONSerialization.jsonObject(with: paired.data) as! [String: Any]
        let access = tokens["accessToken"] as! String
        let refresh = tokens["refreshToken"] as! String
        let components = access.split(separator: ".").map(String.init)
        let claimsData = Data(base64Encoded: components[1].replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/").padding(toLength: (components[1].count + 3) / 4 * 4, withPad: "=", startingAt: 0))!
        let claims = try JSONSerialization.jsonObject(with: claimsData) as! [String: Any]
        try expect(claims["iss"] as? String == "http://0.0.0.0:33333/" && claims["aud"] as? String == "clipboard-app-users", "JWT protocol claims")
        try expect(abs((claims["exp"] as! Double) - Date().timeIntervalSince1970 - 900) < 5, "Access token lifetime")
        var expired = claims; expired["exp"] = 1
        let forgedPayload = try JSONSerialization.data(withJSONObject: expired).base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
        let signingKey = UserDefaults(suiteName: suite)!.string(forKey: "native.jwtSigningKey")!
        let message = components[0] + "." + forgedPayload
        let signature = crypto.hmac(message: message, base64Key: signingKey).replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
        let expiredResult = try await post("/send", ["encryptedContent": "bad"], token: message + "." + signature)
        try expect(expiredResult.status == 401, "Expired signed token is rejected")
        let alteredResult = try await post("/send", ["encryptedContent": "bad"], token: components[0] + "." + forgedPayload + "." + components[2])
        try expect(alteredResult.status == 401, "JWT signature is verified")
        try await eventually("Device persisted") { settings.current.devices.count == 1 }
        let timestamp = ISO8601DateFormatter().string(from: Date())
        let payload: [String: Any] = ["type": "CopiedData", "data": ["type": "Text", "id": 12345, "text": "Android fixture", "dateTime": timestamp, "applicationId": NSNull()]]
        let plain = String(data: try JSONSerialization.data(withJSONObject: payload), encoding: .utf8)!
        let sent = try await post("/send", ["encryptedContent": crypto.seal(plainText: plain, base64Key: secret)], token: access)
        try expect(sent.status == 200, "Encrypted Android payload accepted")
        try await eventually("Incoming clipboard saved") { otherWindow.current.copiedDataList.contains { NativePresentation.shared.text(item: $0) == "Android fixture" } }
        try await sendPayload(["type": "CopiedData", "data": ["type": "FormattedText", "id": 12346, "text": "<b>Android HTML</b>", "mimeType": "text/html", "plainText": "Android HTML", "dateTime": timestamp, "applicationId": NSNull()]], token: access, secret: secret, crypto: crypto)
        try await eventually("Formatted Android payload saved") { otherWindow.current.copiedDataList.contains { NativePresentation.shared.text(item: $0) == "Android HTML" } }
        let rejected = try await post("/send", ["encryptedContent": "bad"], token: access)
        try expect(rejected.status == 400, "Invalid ciphertext rejected")
        let unauthorized = try await post("/send", ["encryptedContent": "bad"], token: refresh)
        try expect(unauthorized.status == 401, "Refresh token cannot authorize sync")
        let refreshed = try await post("/auth/refresh", ["refreshToken": refresh])
        try expect(refreshed.status == 200, "Token refresh")

        let millis = Int64(Date().timeIntervalSince1970 * 1000)
        var notification: [String: Any] = ["notificationKey": "message-fixture", "packageName": "fixture.messenger", "appName": "Messenger", "title": "Fixture", "body": "Message", "postedAt": millis, "updatedAt": millis, "kind": "Message", "actions": [["index": 0, "title": "Reply", "supportsReply": true], ["index": 1, "title": "Archive", "supportsReply": false]]]
        notification["media"] = ["appIcon": imageData.base64EncodedString(), "avatar": imageData.base64EncodedString(), "image": imageData.base64EncodedString()]
        try await sendPayload(["type": "Notification", "notification": notification], token: access, secret: secret, crypto: crypto)
        try await eventually("Notification history and banner") { notificationScreen.current.notifications.count == 1 && notifications.delivered.count == 1 }
        try await sendPayload(["type": "Notification", "notification": notification], token: access, secret: secret, crypto: crypto)
        try await Task.sleep(for: .milliseconds(250))
        try expect(notifications.delivered.count == 1, "Repeated notification does not repeat banners")
        try expect(notificationScreen.current.notifications[0].media?.image == imageData.base64EncodedString() && notificationScreen.current.notifications[0].media?.avatar == imageData.base64EncodedString(), "Notification images and avatars survive transport")
        let socket = try await connect(token: access, secret: secret, crypto: crypto)
        try await eventually("WebSocket connects") { runtime.current.connected }
        let history = try await receivePayload(socket, type: "HistoryCopiedData", secret: secret, crypto: crypto)
        try expect(history["data"] != nil, "WebSocket history snapshot")
        let currentClipboard = try await receivePayload(socket, type: "CurrentClipboard", secret: secret, crypto: crypto)
        try expect((currentClipboard["data"] as? [String: Any])?["plainText"] as? String == "Android HTML", "Snapshot current formatted clipboard")
        try expect(desktop.writeClipboard(content: NativeClipboardContent(kind: "text", text: "Mac outgoing fixture", plainText: "Mac outgoing fixture", paths: [], applicationId: nil)) == nil, "Write outgoing text")
        let outgoing = try await receivePayload(socket, type: "CopiedData", secret: secret, crypto: crypto)
        try expect((outgoing["data"] as? [String: Any])?["text"] as? String == "Mac outgoing fixture", "Native clipboard sync reaches the phone")
        let value = notificationScreen.current.notifications[0]
        notificationScreen.reply(notification: value, index: 0, text: "Native reply")
        let reply = try await receivePayload(socket, type: "NotificationAction", secret: secret, crypto: crypto)
        let request = reply["request"] as! [String: Any]
        let action = request["action"] as! [String: Any]
        try expect(action["text"] as? String == "Native reply" && action["index"] as? Int == 0, "Reply crosses the encrypted WebSocket")
        try await eventually("Reply completes") { notificationScreen.current.pendingActions.isEmpty }
        let reconnected = try await connect(token: access, secret: secret, crypto: crypto)
        try await Task.sleep(for: .milliseconds(200))
        socket.cancel(with: .normalClosure, reason: nil)
        notificationScreen.invoke(notification: value, index: 1)
        let invoked = try await receivePayload(reconnected, type: "NotificationAction", secret: secret, crypto: crypto)
        try expect(((invoked["request"] as? [String: Any])?["action"] as? [String: Any])?["index"] as? Int == 1, "Reconnect preserves the new session")
        reconnected.cancel(with: .normalClosure, reason: nil)
        try await eventually("Disconnected phone") { !runtime.current.connected }
        try expect(runtime.current.serverError == nil, "A disconnected peer does not stop the server")
        try await eventually("Invoke completes") { notificationScreen.current.pendingActions.isEmpty }
        notificationScreen.reply(notification: value, index: 0, text: "Offline")
        try await eventually("Offline action error") { notificationScreen.current.actionError != nil }
        notification["notificationKey"] = "live-fixture"; notification["kind"] = "Progress"; notification["isOngoing"] = true
        try await sendPayload(["type": "Notification", "notification": notification], token: access, secret: secret, crypto: crypto)
        try await eventually("Live Update") { runtime.current.liveUpdate != nil }
        try expect(notifications.delivered.count == 1, "Live Update does not create a duplicate system banner")
        try await sendPayload(["type": "NotificationRemoved", "removal": ["notificationKey": "message-fixture", "removedAt": millis + 1]], token: access, secret: secret, crypto: crypto)
        try await eventually("Remote notification removal") { notificationScreen.current.notifications.first { $0.notificationKey == "message-fixture" }?.isActive == false }
        try expect(notifications.withdrawn.contains("fixture-phone:message-fixture"), "Removed notification withdraws the banner")

        var callbacks = 0
        let subscription = otherWindow.watch { _ in callbacks += 1 }
        subscription.cancel()
        let baseline = callbacks
        settings.addCategory(name: "After cancellation")
        try await Task.sleep(for: .milliseconds(300))
        try expect(callbacks == baseline, "Cancelled subscription has no callbacks")
        settings.cancelPairing()
        clipboard.close(); otherWindow.close(); settings.close(); notificationScreen.close()
        await withCheckedContinuation { continuation in runtime.close { continuation.resume() } }

        let restarted = MacRuntime(desktop: desktop, crypto: crypto, system: notifications, dataDirectory: directory.path, settingsSuite: suite, listenPort: 33333)
        let restored = restarted.clipboardScreen()
        let restoredSettings = restarted.settingsScreen()
        restoredSettings.pair()
        try await eventually("Saved server port is reused") { restoredSettings.qrCode != nil }
        let restoredQR = try JSONSerialization.jsonObject(with: Data(restoredSettings.qrCode!.utf8)) as! [String: Any]
        try expect((restoredQR["port"] as! NSNumber).int32Value == port, "Restart keeps the paired phone's port")
        try await eventually("History restored") { restored.current.copiedDataList.contains { NativePresentation.shared.text(item: $0) == "Android fixture" } }
        try expect(restoredSettings.current.quickAccessHistorySize == 50 && restoredSettings.current.clipboardAutoClearTimeoutDays == 15, "Settings persisted")
        try await eventually("Paired device restored") { restoredSettings.current.devices.count == 1 }
        let restoredNotifications = restarted.notificationsScreen()
        try await eventually("Notification history restored") { restoredNotifications.current.notifications.count == 2 }
        try expect(notifications.delivered.count == 1, "Restart does not redeliver history")
        try expect(restoredNotifications.current.notifications.first { $0.notificationKey == "message-fixture" }?.media?.image == imageData.base64EncodedString(), "Notification image is restored")
        restoredNotifications.clear()
        try await eventually("Clear notification history") { restoredNotifications.current.notifications.isEmpty }
        restoredNotifications.close()
        var finalSocket: URLSessionWebSocketTask?
        for _ in 0..<20 {
            do { finalSocket = try await connect(token: access, secret: secret, crypto: crypto); break }
            catch { try await Task.sleep(for: .milliseconds(100)) }
        }
        guard let finalSocket else { throw TestFailure(message: "Restarted CIO listener becomes available: " + (restarted.current.serverError ?? "no server error")) }
        try await eventually("Connection after restart") { restarted.current.connected }
        restoredSettings.unpair(id: "fixture-phone")
        try await eventually("Unpair disconnects the active phone") { restoredSettings.current.devices.isEmpty && !restarted.current.connected }
        finalSocket.cancel(with: .normalClosure, reason: nil)
        let removedDevice = try await post("/auth/refresh", ["refreshToken": refresh])
        try expect(removedDevice.status == 404, "Unpaired device cannot refresh tokens")
        restored.close(); restoredSettings.close()
        await withCheckedContinuation { continuation in restarted.close { continuation.resume() } }
        print("PASS: CryptoKit, all local clipboard types, categories/exclusions, subscriptions, pairing/sync/refresh, WebSocket/reconnect/replies, notifications/live update/history, persistence, multiple windows, port conflict")
        passed = true
    }

    @MainActor private static func sendPayload(_ payload: [String: Any], token: String, secret: String, crypto: CryptoKitOperations) async throws {
        let text = String(data: try JSONSerialization.data(withJSONObject: payload), encoding: .utf8)!
        let response = try await post("/send", ["encryptedContent": crypto.seal(plainText: text, base64Key: secret)], token: token)
        try expect(response.status == 200, "Payload accepted")
    }
    @MainActor private static func connect(token: String, secret: String, crypto: CryptoKitOperations) async throws -> URLSessionWebSocketTask {
        var request = URLRequest(url: URL(string: "ws://127.0.0.1:\(port)/sync")!)
        request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization")
        let socket = URLSession.shared.webSocketTask(with: request)
        socket.resume()
        let snapshot = String(data: try JSONSerialization.data(withJSONObject: ["type": "SnapshotRequest", "knownCopiedDataIds": []]), encoding: .utf8)!
        let encoded = try JSONSerialization.data(withJSONObject: ["encryptedContent": crypto.seal(plainText: snapshot, base64Key: secret)])
        try await socket.send(.string(String(data: encoded, encoding: .utf8)!))
        return socket
    }
    @MainActor private static func receivePayload(_ socket: URLSessionWebSocketTask, type: String, secret: String, crypto: CryptoKitOperations) async throws -> [String: Any] {
        let timeout = Task { try await Task.sleep(for: .seconds(10)); socket.cancel(with: .goingAway, reason: nil) }
        defer { timeout.cancel() }
        for _ in 0..<60 {
            let message = try await socket.receive()
            let data: Data
            switch message { case .string(let text): data = Data(text.utf8); case .data(let value): data = value; @unknown default: throw TestFailure(message: "Unknown WebSocket frame") }
            let envelope = try JSONSerialization.jsonObject(with: data) as! [String: Any]
            guard let plain = crypto.open(cipherText: envelope["encryptedContent"] as! String, base64Key: secret) else { throw TestFailure(message: "WebSocket decryption") }
            let payload = try JSONSerialization.jsonObject(with: Data(plain.utf8)) as! [String: Any]
            if payload["type"] as? String == "HistoryCopiedData" {
                let kind = (payload["data"] as? [String: Any])?["type"] as? String
                try expect(kind == "Text" || kind == "FormattedText", "Image and file history stays local")
            }
            if payload["type"] as? String == type { return payload }
        }
        throw TestFailure(message: "Missing WebSocket payload \(type)")
    }

    @MainActor private static func post(_ route: String, _ object: [String: Any], token: String? = nil) async throws -> (data: Data, status: Int) {
        var request = URLRequest(url: URL(string: "http://127.0.0.1:\(port)" + route)!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token { request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization") }
        request.httpBody = try JSONSerialization.data(withJSONObject: object)
        let (data, response) = try await URLSession.shared.data(for: request)
        return (data, (response as! HTTPURLResponse).statusCode)
    }
}
