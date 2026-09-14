package windows

import GlobalHotkeyManager
import MacAccessibilityPermission
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.presentation.ClipboardSectionHeader
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationScreen
import com.cross.sync.setting.domain.SettingPreferencesStore
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.AppIcons
import com.cross.sync.theme.icons.CrossSync
import com.cross.sync.theme.icons.LogoNoConnect
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import dev.nucleusframework.composenativetray.tray.api.Tray
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
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64

@Composable
fun ApplicationScope.QuickClipboardWindow(
    openSetting: () -> Unit,
    openHome: () -> Unit,
    globalHotkeyManager: GlobalHotkeyManager,
    pairingState: PairingState?,
    liveUpdateNotification: SyncedNotification? = null,
    onShowLiveUpdateChip: () -> Unit = {},
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
    var quickAccessContent by remember { mutableStateOf(QuickAccessContent.CLIPBOARD) }
    val openNotifications = {
        isTopBar = true
        quickAccessContent = QuickAccessContent.NOTIFICATIONS
        showWindow = true
    }

    Tray(
        icon = if (pairingState is PairingState.Connected) AppIcons().CrossSync else AppIcons().LogoNoConnect,
        tooltip = "Open app",
        tint = null,
        primaryAction = {
            isTopBar = true
            quickAccessContent = QuickAccessContent.CLIPBOARD
            showWindow = !showWindow;
            prevAppId = getFrontmostAppBundleId()
        },
        menuContent = {
            Item(
                label = "Open app",
                onClick = openHome
            )
            Item(
                label = "Notifications",
                onClick = openNotifications,
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

    liveUpdateNotification?.let { notification ->
        val tooltip = notification.statusBarTooltip()
        val notificationIcon = remember(notification.media?.notificationIcon) {
            notification.media?.notificationIcon?.toImageBitmap()
        }
        if (notificationIcon == null) {
            Tray(
                icon = AppIcons().CrossSync,
                tooltip = tooltip,
                primaryAction = onShowLiveUpdateChip,
                menuContent = null,
            )
        } else {
            Tray(
                icon = BitmapPainter(notificationIcon),
                tooltip = tooltip,
                primaryAction = onShowLiveUpdateChip,
                menuContent = null,
            )
        }
    }

    DisposableEffect(Unit) {
        val showClipboardContentAction = {
            isTopBar = false
            quickAccessContent = QuickAccessContent.CLIPBOARD
            prevAppId = getFrontmostAppBundleId()
            showWindow = !showWindow
        }

        val hideWindowAction = {
            showWindow = false
        }

        globalHotkeyManager.registerHotkey("meta shift V") {
            showClipboardContentAction()
        }

        var escapeHotkeyRegistered = tryRegisterGlobalHotkey(onHotkey = hideWindowAction) { pressedKeys ->
            pressedKeys.contains(NativeKeyEvent.VC_ESCAPE)
        }

        // Permission can be granted while System Settings is open. Retry once when the app
        // becomes trusted, so the user does not need to restart CrossSync.
        val permissionJob = if (!escapeHotkeyRegistered && MacAccessibilityPermission.isMacOs) {
            coroutineScope.launch {
                while (!MacAccessibilityPermission.isGranted()) {
                    delay(1_000)
                }
                escapeHotkeyRegistered = tryRegisterGlobalHotkey(onHotkey = hideWindowAction) { pressedKeys ->
                    pressedKeys.contains(NativeKeyEvent.VC_ESCAPE)
                }
            }
        } else {
            null
        }

        onDispose {
            permissionJob?.cancel()
            globalHotkeyManager.stop()

            if (escapeHotkeyRegistered) {
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
                    when (quickAccessContent) {
                        QuickAccessContent.CLIPBOARD -> ClipboardScreen(
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
                        onOpenNotifications = {
                            quickAccessContent = QuickAccessContent.NOTIFICATIONS
                        },
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

                        QuickAccessContent.NOTIFICATIONS -> NotificationScreen(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    0.1.dp,
                                    color = AppTheme.colors.outline.copy(alpha = 0.2f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                )
                                .background(AppTheme.colors.background),
                            onBack = { quickAccessContent = QuickAccessContent.CLIPBOARD },
                            compact = true,
                        )
                    }
                }
            }

        }
    }
}

private fun SyncedNotification.statusBarTooltip(): String {
    val title = title.trim().ifBlank { "New notification" }
    val body = body.trim()
    return buildString {
        append(appName.trim().ifBlank { packageName })
        append(": ")
        append(title)
        if (body.isNotBlank()) {
            append(" — ")
            append(body)
        }
    }.replace(Regex("\\s+"), " ").take(256)
}

private fun String.toImageBitmap(): ImageBitmap? = runCatching {
    SkiaImage.makeFromEncoded(Base64.decode(this)).toComposeImageBitmap()
}.getOrNull()

private enum class QuickAccessContent { CLIPBOARD, NOTIFICATIONS }
