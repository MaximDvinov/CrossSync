import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.App
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun getMouseAWTPosition(): Pair<Int, Int> {
    val point = MouseInfo.getPointerInfo().location
    return point.x to point.y
}

fun findScreenBoundsContaining(x: Int, y: Int): Rectangle {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val devices = ge.screenDevices
    for (device in devices) {
        val bounds = device.defaultConfiguration.bounds
        if (bounds.contains(x, y)) return bounds
    }
    return ge.defaultScreenDevice.defaultConfiguration.bounds
}

fun clampToBounds(
    x: Int,
    y: Int,
    bounds: Rectangle,
    windowWidthPx: Int,
    windowHeightPx: Int,
): Pair<Int, Int> {
    var nx = x
    var ny = y
    return nx to ny
}

class GlobalHotkeyListener(
    private val onHotkey: () -> Unit,
) : NativeKeyListener {
    private var cmdPressed = false
    private var shiftPressed = false

    override fun nativeKeyTyped(e: NativeKeyEvent?) { /* no-op */
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        when (e.keyCode) {
            NativeKeyEvent.VC_SHIFT -> shiftPressed = true
            NativeKeyEvent.VC_META -> cmdPressed = true // macOS Command
            NativeKeyEvent.VC_V -> {
                if (cmdPressed && shiftPressed) {
                    onHotkey()
                }
            }
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        when (e.keyCode) {
            NativeKeyEvent.VC_SHIFT -> shiftPressed = false
            NativeKeyEvent.VC_META -> cmdPressed = false
        }
    }
}

/**
 * Попытка зарегистрировать глобальный хук.
 * Возвращает true если зарегистрировано, иначе false.
 * В случае macOS — показывает диалог с инструкциями по включению Accessibility.
 */
fun tryRegisterGlobalHotkey(onHotkey: () -> Unit): Boolean {
    return try {
        Logger.getLogger(GlobalScreen::class.java.getPackage().name).level = Level.OFF
        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(GlobalHotkeyListener(onHotkey))
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
    } catch (_: Exception) { /* ignore */
    }
}

fun bringAppWindowsToFront() {
    SwingUtilities.invokeLater {
        java.awt.Window.getWindows().forEach { w ->
            try {
                if (w.isVisible) {
                    w.toFront()
                    w.requestFocus()
                }
            } catch (_: Throwable) { /* ignore individual failures */
            }
        }
    }
}

fun bringAppToFront() {
    try {
        Runtime.getRuntime().exec(
            arrayOf(
                "osascript",
                "-e",
                "tell application \"CrossSync\" to activate"
            )
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun calculateWindowPositionUnderMouse(
    windowStateWidthDp: androidx.compose.ui.unit.Dp,
    windowStateHeightDp: androidx.compose.ui.unit.Dp,
    density: androidx.compose.ui.unit.Density
): WindowPosition {
    val (mx, my) = getMouseAWTPosition()
    val bounds = findScreenBoundsContaining(mx, my)
    val offsetY = 8 // px ниже курсора
    val windowWidthPx = with(density) { windowStateWidthDp.toPx() }.toInt()
    val windowHeightPx = with(density) { windowStateHeightDp.toPx() }.toInt()

    var desiredX = mx - windowWidthPx / 4 // центрируем по X
    var desiredY = my - windowHeightPx / 4

//    val (nx, ny) = clampToBounds(desiredX, desiredY, bounds, windowWidthPx, windowHeightPx)

    println("bounds: $bounds")
    println("desiredX: $desiredX, desiredY: $desiredY")
    println("nx: $desiredX, ny: $desiredY")

    return WindowPosition.Absolute((desiredX).dp, (desiredY).dp)
}

fun main() = application {
    val windowWidthDp = 300.dp
    val windowHeightDp = 400.dp

    var showWindow by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)

    // регистрация глобального хоткея (DisposableEffect чтобы отписаться при выходе)
    DisposableEffect(Unit) {
        val toggleAction = {
            showWindow = !showWindow
        }
        val registered = tryRegisterGlobalHotkey(toggleAction)
        onDispose {
            if (registered) unregisterGlobalHotkeyIfRegistered()
        }
    }

    val density = LocalDensity.current

    LaunchedEffect(showWindow) {
        if (showWindow) {
            windowState.position = calculateWindowPositionUnderMouse(windowWidthDp, windowHeightDp, density)

            // поднимаем окно наверх и запрашиваем фокус
            bringAppToFront()
            bringAppWindowsToFront()
        }
    }

    Window(
        title = "CrossSync",
        state = windowState,
        visible = showWindow,
        onCloseRequest = { showWindow = false }
    ) {
        App()
    }
}
