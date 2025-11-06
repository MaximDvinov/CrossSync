import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
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
import java.awt.Robot
import java.awt.event.KeyEvent
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
}

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
fun main() = application {
    KoinApplication({
        modules(desktopModule, clipboardModule)
    }) {
        val initClipboardManagerUseCase = koinInject<InitClipboardManagerUseCase>()
        LaunchedEffect(Unit) {
            initClipboardManagerUseCase.invoke()
        }
        val observeCopiedDataUseCase = koinInject<ObserveCopiedDataUseCase>()
        val setCopiedData = koinInject<AddCopiedDataUseCase>()

        val windowWidthDp = 400.dp
        val windowHeightDp = 500.dp

        var showWindow by remember { mutableStateOf(false) }
        val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)
        var prevAppId by remember { mutableStateOf<String?>(null) }


        DisposableEffect(Unit) {
            val showClipboardContentAction = {
                prevAppId = getFrontmostAppBundleId()
                showWindow = !showWindow
            }

            val hideWindowAction = {
                showWindow = false
            }

            val showClipboardHotKey =
                tryRegisterGlobalHotkey(
                    GlobalHotkeyListener(showClipboardContentAction) { pressedKeys ->
                        pressedKeys.contains(NativeKeyEvent.VC_SHIFT) &&
                                pressedKeys.contains(NativeKeyEvent.VC_META) &&
                                pressedKeys.contains(NativeKeyEvent.VC_V)
                    },
                    GlobalHotkeyListener(hideWindowAction) { pressedKeys ->
                        pressedKeys.contains(NativeKeyEvent.VC_ESCAPE)
                    }
                )

            onDispose {
                if (showClipboardHotKey) unregisterGlobalHotkeyIfRegistered()
            }
        }

        val density = LocalDensity.current

        LaunchedEffect(showWindow) {
            if (showWindow) {
                windowState.position =
                    calculateWindowPositionUnderMouse(windowWidthDp, windowHeightDp, density)
            }
        }

        Tray(
            icon = rememberVectorPainter(FeatherIcons.Copy),
            onAction = { showWindow = !showWindow },
            state = rememberTrayState()
        )

        val clipboardData by observeCopiedDataUseCase().collectAsState(listOf())

        Window(
            title = "CrossSync",
            state = windowState,
            visible = showWindow,
            alwaysOnTop = true,
            onCloseRequest = { showWindow = false },
//            undecorated = true
        ) {
            val coroutineScope = rememberCoroutineScope()
            Column(
                modifier = Modifier.fillMaxWidth().safeContentPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                clipboardData.takeLast(40).reversed().forEach {
                    BasicText(
                        (it as CopiedData.Text).text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable {
                            coroutineScope.launch {
                                setCopiedData(it)
                                val robot = Robot()
                                val isMac =
                                    System.getProperty("os.name").lowercase().contains("mac")
                                val modifier = if (isMac) KeyEvent.VK_META else KeyEvent.VK_CONTROL

                                showWindow = false

                                prevAppId?.let { it1 -> bringAppToFront(it1) }

                                pasteClipboardMac()
                            }
                        },
                        style = TextStyle(color = Color.Black, fontSize = 16.sp)
                    )
                }

                prevAppId?.let { text ->
                    BasicText(
                        text = text,
                    )
                }
            }
        }
    }
}

