package windows

import GlobalHotkeyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.theme.icons.AppIcons
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.CrossSync
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.kdroid.composetray.tray.api.Tray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tryRegisterGlobalHotkey
import unregisterGlobalHotkeyIfRegistered
import utils.bringAppToFront
import utils.calculateWindowPositionUnderMouse
import utils.getFrontmostAppBundleId
import utils.pasteClipboardMac
import java.awt.Dimension

@Composable
fun ApplicationScope.QuickClipboardWindow(
    openSetting: () -> Unit,
    openHome: () -> Unit,
    globalHotkeyManager: GlobalHotkeyManager
) {
    val density = LocalDensity.current

    val windowWidthDp = 350.dp
    val windowHeightDp = 500.dp
    var isTopBar by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)
    var showWindow by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var prevAppId by remember { mutableStateOf<String?>(null) }

    Tray(
        icon = AppIcons().CrossSync,
        tooltip = "Open app",
        tint = null,
        primaryAction = {
            isTopBar = true
            showWindow = !showWindow;
            prevAppId = getFrontmostAppBundleId()
        },
        menuContent = {
            Item(
                label = "Open app",
                onClick = openHome
            )
            Item(
                label = "Setting",
                onClick = openSetting
            )
            Item(
                label = "Close",
                onClick = {
                    exitApplication()
                }
            )
        }
    )

    DisposableEffect(Unit) {
        val showClipboardContentAction = {
            isTopBar = false
            prevAppId = getFrontmostAppBundleId()
            showWindow = !showWindow
        }

        val hideWindowAction = {
            showWindow = false
        }

        globalHotkeyManager.registerHotkey("meta shift V") {
            showClipboardContentAction()
        }

        val escapeHotkey = tryRegisterGlobalHotkey(onHotkey = hideWindowAction) { pressedKeys ->
            pressedKeys.contains(NativeKeyEvent.VC_ESCAPE)
        }

        onDispose {
            globalHotkeyManager.stop()

            if (escapeHotkey) {
                unregisterGlobalHotkeyIfRegistered()
            }
        }
    }

    var isWindowShowed by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(showWindow) {
        if (showWindow) {
            windowState.position =
                calculateWindowPositionUnderMouse(
                    windowWidthDp,
                    windowHeightDp,
                    density,
                    isTopBar
                )
        }
        delay(10)
        isWindowShowed = showWindow
    }

    Window(
        title = "CrossSync",
        state = windowState,
        visible = isWindowShowed,
        alwaysOnTop = true,
        onCloseRequest = {
            showWindow = false
        },
        undecorated = true,
        transparent = true,
    ) {
        this.window.minimumSize =
            Dimension(windowWidthDp.value.toInt(), windowHeightDp.value.toInt())

        WindowDraggableArea {
            AppTheme {
                Box {
                    ClipboardScreen(
                        modifier = Modifier.Companion
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                0.1.dp,
                                color = Color(0x3300253C),
                                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            )
                            .background(AppTheme.colors.background),
                        onClose = {
                            showWindow = false
                        },
                        onOpenFullApp = openHome,
                    ) {
                        coroutineScope.launch {
                            showWindow = false
                            prevAppId?.let { bringAppToFront(it) }
                            pasteClipboardMac()
                        }
                    }
                }
            }

        }
    }
}