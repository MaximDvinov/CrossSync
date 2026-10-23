package utils

import MacAccessibilityPermission
import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import notifications.macOsNotificationBridgeFile

internal enum class MacPasteResult { SENT, NO_TARGET, UNAVAILABLE, NOT_TRUSTED, ACTIVATION_FAILED, TIMED_OUT }

internal interface MacPastePlatform {
    fun activate(pid: Int): Boolean
    fun canPaste(pid: Int): Boolean
    fun postPaste(pid: Int): Int
}

internal class MacPasteController(private val platform: MacPastePlatform) {
    suspend fun paste(pid: Int): MacPasteResult {
        currentCoroutineContext().ensureActive()
        if (pid <= 0) return MacPasteResult.NO_TARGET
        if (!platform.activate(pid)) return MacPasteResult.ACTIVATION_FAILED
        return withTimeoutOrNull(1_500) { awaitPaste(pid) } ?: MacPasteResult.TIMED_OUT
    }

    private suspend fun awaitPaste(pid: Int): MacPasteResult {
        while (true) {
            if (platform.canPaste(pid)) {
                // Let activation settle, then check again in the native send operation.
                delay(100)
                when (platform.postPaste(pid)) {
                    1 -> return MacPasteResult.SENT
                    -1 -> return MacPasteResult.NOT_TRUSTED
                    -2 -> return MacPasteResult.UNAVAILABLE
                }
            }
            delay(25)
        }
    }
}

internal object MacOsPaste {
    private val library: MacPasteNativeLibrary? by lazy {
        if (!MacAccessibilityPermission.isMacOs) return@lazy null
        val file = macOsNotificationBridgeFile() ?: return@lazy null
        runCatching { Native.load(file.absolutePath, MacPasteNativeLibrary::class.java) }
            .onFailure { println("CrossSync: paste bridge could not load: ${it.message}") }
            .getOrNull()
    }

    fun captureTarget(): Int? = library?.crosssync_frontmost_application_pid()?.takeIf { it > 0 }

    fun restoreFocus(pid: Int?) {
        if (pid != null) library?.crosssync_activate_application(pid)
    }

    suspend fun paste(pid: Int?): MacPasteResult {
        if (pid == null) return MacPasteResult.NO_TARGET
        val nativeLibrary = library ?: return MacPasteResult.UNAVAILABLE
        if (!MacAccessibilityPermission.isGranted()) {
            MacAccessibilityPermission.showSettingsPromptOnce()
            return MacPasteResult.NOT_TRUSTED
        }
        val result = MacPasteController(object : MacPastePlatform {
            override fun activate(pid: Int) = nativeLibrary.crosssync_activate_application(pid) == 1
            override fun canPaste(pid: Int) = nativeLibrary.crosssync_can_paste(pid) == 1
            override fun postPaste(pid: Int) = nativeLibrary.crosssync_post_paste(pid)
        }).paste(pid)
        if (result == MacPasteResult.NOT_TRUSTED) MacAccessibilityPermission.showSettingsPromptOnce()
        return result
    }
}

private interface MacPasteNativeLibrary : Library {
    fun crosssync_frontmost_application_pid(): Int
    fun crosssync_activate_application(pid: Int): Int
    fun crosssync_can_paste(pid: Int): Int
    fun crosssync_post_paste(pid: Int): Int
}
