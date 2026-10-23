package com.cross.sync.macos

/** OS operations only. History, filtering and synchronization remain in Kotlin. */
interface DesktopServices {
    fun readClipboard(): NativeClipboardContent?
    fun clipboardRevision(): Int
    /** null means success; failure must leave the current pasteboard intact. */
    fun writeClipboard(content: NativeClipboardContent): String?
    fun installedApplications(): List<NativeApplication>
    fun lanAddresses(): List<String>
    fun deviceName(): String
    fun autoStartEnabled(): Boolean
    /** null means success; otherwise return a user-visible error. */
    fun setAutoStart(enabled: Boolean): String?
}

data class NativeApplication(val id: String, val name: String, val icon: String?, val path: String)
data class NativeClipboardContent(
    val kind: String,
    val text: String,
    val plainText: String,
    val paths: List<String>,
    val applicationId: String?,
)

/** CryptoKit adapter; does not own tokens, protocol validation or device state. */
interface CryptoOperations {
    fun generateKey(): String
    fun seal(plainText: String, base64Key: String): String
    fun open(cipherText: String, base64Key: String): String?
    fun hmac(message: String, base64Key: String): String
}
