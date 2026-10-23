import Foundation
import CryptoKit
@preconcurrency import CrossSyncShared

final class CryptoKitOperations: NSObject, CryptoOperations {
    func generateKey() -> String {
        SymmetricKey(size: .bits256).withUnsafeBytes { Data($0).base64EncodedString() }
    }
    func seal(plainText: String, base64Key: String) -> String {
        guard let key = Data(base64Encoded: base64Key), key.count == 32,
              let box = try? AES.GCM.seal(Data(plainText.utf8), using: SymmetricKey(data: key)),
              let combined = box.combined else { return "" }
        return combined.base64EncodedString()
    }
    func open(cipherText: String, base64Key: String) -> String? {
        guard let key = Data(base64Encoded: base64Key), key.count == 32,
              let combined = Data(base64Encoded: cipherText), combined.count >= 28,
              let box = try? AES.GCM.SealedBox(combined: combined),
              let plain = try? AES.GCM.open(box, using: SymmetricKey(data: key)) else { return nil }
        return String(data: plain, encoding: .utf8)
    }
    func hmac(message: String, base64Key: String) -> String {
        guard let key = Data(base64Encoded: base64Key) else { return "" }
        let signature = HMAC<SHA256>.authenticationCode(for: Data(message.utf8), using: SymmetricKey(data: key))
        return Data(signature).base64EncodedString()
    }
}
