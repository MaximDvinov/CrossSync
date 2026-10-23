package utils

import MacAccessibilityPermission
import com.sun.jna.Library
import com.sun.jna.Native
import notifications.macOsNotificationBridgeFile

internal object MacOsClipboardRevision {
    private val library: ClipboardRevisionLibrary? by lazy {
        if (!MacAccessibilityPermission.isMacOs) return@lazy null
        val file = macOsNotificationBridgeFile() ?: return@lazy null
        runCatching { Native.load(file.absolutePath, ClipboardRevisionLibrary::class.java) }.getOrNull()
    }

    fun current(): Long? = runCatching { library?.crosssync_clipboard_change_count() }.getOrNull()
}

private interface ClipboardRevisionLibrary : Library {
    fun crosssync_clipboard_change_count(): Long
}
