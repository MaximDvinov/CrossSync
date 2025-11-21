import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.tulskiy.keymaster.common.Provider
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JOptionPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities


class GlobalHotkeyManager(
    private val provider: Provider,
) {
    fun registerHotkey(
        hotkey: String,
        onHotkey: () -> Unit,
    ) {
        provider.register(KeyStroke.getKeyStroke(hotkey)) { onHotkey() }
    }

    fun registerHotkey(
        hotkey: KeyStroke,
        onHotkey: () -> Unit,
    ) {
        provider.register(hotkey) { onHotkey() }
    }

    fun stop() {
        provider.reset()
        provider.stop()
    }
}


fun tryRegisterGlobalHotkey(
    onHotkey: () -> Unit,
    pattern: (pressedKeys: Set<Int>) -> Boolean,
): Boolean {
    return try {
        Logger.getLogger(GlobalScreen::class.java.getPackage().name).level = Level.OFF
        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook()
        }

        GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
            private val pressedKeys = mutableSetOf<Int>()

            override fun nativeKeyPressed(e: NativeKeyEvent) {
                pressedKeys.add(e.keyCode)
                if (pattern(pressedKeys)) {
                    onHotkey()
                }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent) {
                pressedKeys.remove(e.keyCode)
            }
        })

        true
    } catch (ex: NativeHookException) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val message = if (os.contains("mac")) {
            """
            Не удалось зарегистрировать глобальный хук клавиатуры.
            
            На macOS требуется разрешение "Accessibility" (Универсальный доступ) для приложения.
            Чтобы разрешить:
            
            1) Откройте System Settings → Privacy & Security → Accessibility (Универсальный доступ).
            2) Нажмите замок и разблокируйте изменения.
            3) Добавьте ваше приложение (или Android Studio, если вы запускаете из IDE).
            4) Перезапустите приложение.
            
            Детали ошибки: ${ex.message}
            """.trimIndent()
        } else {
            "Не удалось зарегистрировать глобальный хук: ${ex.message}"
        }
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                null,
                message,
                "Глобальный хоткей не зарегистрирован",
                JOptionPane.WARNING_MESSAGE
            )
        }
        false
    }
}

fun unregisterGlobalHotkeyIfRegistered() {
    try {
        if (GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.unregisterNativeHook()
        }
    } catch (_: Exception) {
    }
}