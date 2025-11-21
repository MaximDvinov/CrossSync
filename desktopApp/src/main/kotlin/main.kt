import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.usecase.SaveApplicationsUseCase
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.syncing.di.syncingModule
import com.cross.sync.syncing.domain.usecases.StartSyncUseCase
import com.cross.sync.theme.AppIcons
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.CrossSync
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.IconRenderProperties
import com.tulskiy.keymaster.common.Provider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import utils.bringAppToFront
import utils.calculateWindowPositionUnderMouse
import utils.getFrontmostAppBundleId
import utils.getInstalledApplications
import utils.pasteClipboardMac
import java.awt.Dimension
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyManager)
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication({
        modules(desktopModule, syncingModule, clipboardModule)
    }) {
        val globalHotkeyManager = koinInject<GlobalHotkeyManager>()
        val saveApplicationsUseCase = koinInject<SaveApplicationsUseCase>()
        val startSyncUseCase = koinInject<StartSyncUseCase>()

        val density = LocalDensity.current
        val windowWidthDp = 350.dp
        val windowHeightDp = 500.dp

        LaunchedEffect(Unit) {
            launch {
                val applications = getInstalledApplications() + Application(
                    id = "com.apple.finder",
                    name = "Finder",
                    null,
                    ""
                )
                saveApplicationsUseCase(applications)
            }
        }

        LaunchedEffect(Unit) {
            startSyncUseCase()
        }

        var showWindow by remember { mutableStateOf(false) }
        var isTopBar by remember { mutableStateOf(false) }
        val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)
        var prevAppId by remember { mutableStateOf<String?>(null) }

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
//                Item(
//                    label = "Open app",
//                    onClick = {
//
//                    }
//                )
//                Item(
//                    label = "Setting",
//                    onClick = {
//
//                    }
//                )
                Item(
                    label = "Close",
                    onClick = {
                        exitApplication()
                    }
                )
            }
        )

        val coroutineScope = rememberCoroutineScope()

        Window(
            title = "CrossSync",
            state = windowState,
            visible = isWindowShowed,
            alwaysOnTop = true,
            onCloseRequest = { showWindow = false },
            undecorated = true,
            transparent = true,
        ) {
            this.window.minimumSize =
                Dimension(windowWidthDp.value.toInt(), windowHeightDp.value.toInt())

            var ram by remember {
                mutableStateOf(getHeapMemoryUsage())
            }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1.seconds)
                    ram = getHeapMemoryUsage()
                }
            }

            WindowDraggableArea {
                AppTheme{
                    Box {
                        ClipboardScreen(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    0.1.dp,
                                    color = Color(0x3300253C),
                                    RoundedCornerShape(20.dp)
                                )
                                .background(AppTheme.colors.background)
                            ,
                            onClose = {
                                showWindow = false
                            },
                            onOpenFullApp = {},
                        ) {
                            coroutineScope.launch {
                                showWindow = false
                                prevAppId?.let { bringAppToFront(it) }
                                pasteClipboardMac()
                            }
                        }

//                    BasicText(
//                        text = ram,
//                        style = TextStyle(fontSize = 8.sp),
//                        modifier = Modifier.alpha(0.6f).padding(10.dp)
//                            .clip(RoundedCornerShape(4.dp))
//                            .background(Color.LightGray).padding(4.dp)
//                            .align(Alignment.BottomEnd),
//                    )
                    }
                }

            }
        }
    }
}

fun getHeapMemoryUsage(): String {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val free = runtime.freeMemory() / 1024 / 1024
    val total = runtime.totalMemory() / 1024 / 1024
    val max = runtime.maxMemory() / 1024 / 1024
    return "RAM (MB): used: $used, free: $free, total: $total, max: $max"
}
