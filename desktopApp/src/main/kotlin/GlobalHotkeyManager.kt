import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import com.tulskiy.keymaster.common.Provider
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.KeyStroke


class GlobalHotkeyManager(
    private val provider: Provider,
) {
    fun registerHotkey(
        hotkey: String,
        onHotkey: () -> Unit,
    ): Boolean = registerHotkey(KeyStroke.getKeyStroke(hotkey), onHotkey)

    fun registerHotkey(
        hotkey: KeyStroke,
        onHotkey: () -> Unit,
    ): Boolean = runCatching {
        provider.register(hotkey) { onHotkey() }
        true
    }.getOrElse {
        MacAccessibilityPermission.showSettingsPromptOnce()
        false
    }

    fun stop() {
        runCatching { provider.reset() }
        runCatching { provider.stop() }
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
    } catch (_: NativeHookException) {
        MacAccessibilityPermission.showSettingsPromptOnce()
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

fun registerGlobalMousePressListener(
    onMousePressed: (x: Int, y: Int) -> Unit,
): NativeMouseListener? {
    return try {
        Logger.getLogger(GlobalScreen::class.java.getPackage().name).level = Level.OFF
        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook()
        }

        val listener = object : NativeMouseListener {
            override fun nativeMouseClicked(e: NativeMouseEvent) = Unit

            override fun nativeMousePressed(e: NativeMouseEvent) {
                onMousePressed(e.x, e.y)
            }

            override fun nativeMouseReleased(e: NativeMouseEvent) = Unit
        }
        GlobalScreen.addNativeMouseListener(listener)
        listener
    } catch (_: NativeHookException) {
        null
    }
}

fun unregisterGlobalMousePressListener(listener: NativeMouseListener?) {
    listener ?: return
    runCatching { GlobalScreen.removeNativeMouseListener(listener) }
}
