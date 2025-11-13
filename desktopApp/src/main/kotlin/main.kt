import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.usecase.SaveApplicationsUseCase
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.IconRenderProperties
import com.tulskiy.keymaster.common.Provider
import compose.icons.FeatherIcons
import compose.icons.feathericons.Copy
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
import java.awt.Toolkit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyManager)
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication({
        modules(desktopModule, clipboardModule)
    }) {
        val screenSize = remember { Toolkit.getDefaultToolkit().screenSize }
        val density = LocalDensity.current
        val globalHotkeyManager = koinInject<GlobalHotkeyManager>()
        val applicationsUseCase = koinInject<SaveApplicationsUseCase>()
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
                applicationsUseCase(applications)
            }
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
            icon = FeatherIcons.Copy,
            tooltip = "Open app",
            tint = null,
            primaryAction = {
                isTopBar = true
                showWindow = !showWindow;
                prevAppId = getFrontmostAppBundleId()
            },
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
                Box {
                    ClipboardScreen(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                0.1.dp,
                                color = Color(0x3300253C),
                                RoundedCornerShape(20.dp)
                            ),
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

fun getHeapMemoryUsage(): String {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val free = runtime.freeMemory() / 1024 / 1024
    val total = runtime.totalMemory() / 1024 / 1024
    val max = runtime.maxMemory() / 1024 / 1024
    return "RAM (MB): used: $used, free: $free, total: $total, max: $max"
}
