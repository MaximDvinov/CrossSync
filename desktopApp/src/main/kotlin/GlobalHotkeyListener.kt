import com.tulskiy.keymaster.common.Provider
import javax.swing.KeyStroke


class GlobalHotkeyListener(
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