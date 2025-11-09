import androidx.compose.foundation.border
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
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.kdroid.composetray.tray.api.Tray
import com.tulskiy.keymaster.common.Provider
import compose.icons.FeatherIcons
import compose.icons.feathericons.Copy
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import utils.bringAppToFront
import utils.calculateWindowPositionUnderMouse
import utils.getFrontmostAppBundleId
import utils.pasteClipboardMac
import java.awt.Dimension
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyListener)
}

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication({
        modules(desktopModule, clipboardModule)
    }) {
        val globalHotkeyListener = koinInject<GlobalHotkeyListener>()
        val windowWidthDp = 400.dp
        val windowHeightDp = 500.dp

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

            globalHotkeyListener.registerHotkey("meta shift V") {
                showClipboardContentAction()
            }

//            globalHotkeyListener.registerHotkey("ESCAPE") {
//                hideWindowAction()
//            }

            onDispose {
                globalHotkeyListener.stop()
            }
        }

        val density = LocalDensity.current

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
            visible = showWindow,
            alwaysOnTop = true,
            onCloseRequest = { showWindow = false },
            undecorated = true,
            transparent = true,
        ) {
            this.window.minimumSize =
                Dimension(windowWidthDp.value.toInt(), windowHeightDp.value.toInt())

            WindowDraggableArea {
                ClipboardScreen(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .border(0.1.dp, color = Color(0x3300253C), RoundedCornerShape(20.dp))
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

