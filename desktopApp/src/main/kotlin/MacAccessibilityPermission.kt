import com.sun.jna.NativeLibrary
import java.awt.Desktop
import java.awt.EventQueue
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane

/** Handles the macOS permission required by JNativeHook to observe global keys. */
object MacAccessibilityPermission {
    val isMacOs: Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    fun isGranted(): Boolean {
        if (!isMacOs) return true

        // If the check itself is unavailable, keep JNativeHook as the source of truth.
        return runCatching {
            applicationServices
                .getFunction("AXIsProcessTrusted")
                .invoke(Boolean::class.java, emptyArray<Any>()) as Boolean
        }.getOrDefault(true)
    }

    fun showSettingsPromptOnce() {
        if (!isMacOs || !promptShown.compareAndSet(false, true)) return

        EventQueue.invokeLater {
            openAccessibilitySettings()
            JOptionPane.showMessageDialog(
                null,
                "Разрешите CrossSync в разделе «Универсальный доступ», чтобы работали глобальные сочетания.",
                "Глобальные сочетания",
                JOptionPane.INFORMATION_MESSAGE,
            )
        }
    }

    private fun openAccessibilitySettings() {
        runCatching {
            check(Desktop.isDesktopSupported())
            Desktop.getDesktop().browse(URI(ACCESSIBILITY_SETTINGS_URI))
        }.recoverCatching {
            ProcessBuilder("open", ACCESSIBILITY_SETTINGS_URI).start()
        }
    }

    private val applicationServices by lazy {
        NativeLibrary.getInstance("/System/Library/Frameworks/ApplicationServices.framework/ApplicationServices")
    }

    private val promptShown = AtomicBoolean(false)

    private const val ACCESSIBILITY_SETTINGS_URI =
        "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"
}
