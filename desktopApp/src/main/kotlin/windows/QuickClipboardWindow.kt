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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.presentation.ClipboardSectionHeader
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.AppIcons
import com.cross.sync.theme.icons.CrossSync
import com.cross.sync.theme.icons.LogoNoConnect
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.kdroid.composetray.tray.api.Tray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tryRegisterGlobalHotkey
import unregisterGlobalHotkeyIfRegistered
import utils.bringAppToFront
import utils.calculateWindowLocationUnderMouse
import utils.getFrontmostAppBundleId
import utils.pasteClipboardMac
import java.awt.Dimension
import java.awt.Window as AwtWindow
import org.koin.compose.koinInject

@Composable
fun ApplicationScope.QuickClipboardWindow(
    openSetting: () -> Unit,
    openHome: () -> Unit,
    globalHotkeyManager: GlobalHotkeyManager,
    pairingState: PairingState?,
) {
    val settingPreferencesStore = koinInject<SettingPreferencesStore>()
    val density = LocalDensity.current

    val windowWidthDp = 350.dp
    val windowHeightDp = 500.dp
    var isTopBar by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)
    var showWindow by remember { mutableStateOf(false) }
    var awtWindow by remember { mutableStateOf<AwtWindow?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var prevAppId by remember { mutableStateOf<String?>(null) }
    var quickAccessHistorySize by remember {
        mutableStateOf(settingPreferencesStore.getGeneralSettings().quickAccessHistorySize)
    }

    Tray(
        icon = if (pairingState is PairingState.Connected) AppIcons().CrossSync else AppIcons().LogoNoConnect,
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
            quickAccessHistorySize =
                settingPreferencesStore.getGeneralSettings().quickAccessHistorySize
            val location = calculateWindowLocationUnderMouse(
                windowWidthDp,
                windowHeightDp,
                density,
                isTopBar
            )

            // On macOS a hidden native window keeps the display it was last shown on.
            // Move the native window before it becomes visible, then repeat after showing it.
            awtWindow?.location = location
            isWindowShowed = true
            delay(10)
            awtWindow?.location = location
        } else {
            isWindowShowed = false
        }
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
        val nativeWindow = this.window
        DisposableEffect(nativeWindow) {
            awtWindow = nativeWindow
            onDispose {
                if (awtWindow === nativeWindow) awtWindow = null
            }
        }

        this.window.minimumSize =
            Dimension(windowWidthDp.value.toInt(), windowHeightDp.value.toInt())

        WindowDraggableArea {
            AppTheme {
                Box {
                    ClipboardScreen(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                0.1.dp,
                                color = AppTheme.colors.outline.copy(alpha = 0.2f),
                                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            )
                            .background(AppTheme.colors.background),
                        onClose = {
                            showWindow = false
                        },
                        onOpenFullApp = openHome,
                        isLargeControls = false,
                        maxVisibleItems = quickAccessHistorySize,
                        showClearAllButton = true,
//                        headerContent = {
//                            ClipboardSectionHeader(
//                                title = "Quick Access",
//                                description = "Recent copied items for fast paste.",
//                                horizontalPadding = 10.dp
//                            )
//                        },
                        onPaste = {
                            coroutineScope.launch {
                                showWindow = false
                                prevAppId?.let { bringAppToFront(it) }
                                pasteClipboardMac()
                            }
                        }
                    )
                }
            }

        }
    }
}
