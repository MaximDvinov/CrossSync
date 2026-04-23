package windows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.cross.sync.clipboard.presentation.ClipboardSectionHeader
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.setting.presentation.SettingScreen
import com.cross.sync.theme.AppTheme

enum class GeneralWindowState {
    GENERAL, SETTING, CLOSE
}

@Composable
fun ApplicationScope.GeneralWindow(
    generalWindowState: GeneralWindowState,
    closeWindow: () -> Unit
) {
    val windowWidthDp = 600.dp
    val windowHeightDp = 800.dp
    val windowState = rememberWindowState(width = windowWidthDp, height = windowHeightDp)

    if (generalWindowState != GeneralWindowState.CLOSE) {
        Window(
            title = "CrossSync",
            state = windowState,
            onCloseRequest = closeWindow,
            undecorated = true,
            transparent = true,
        ) {
            var currentScreen by remember { mutableStateOf(generalWindowState) }
            var openedFromGeneral by remember { mutableStateOf(generalWindowState == GeneralWindowState.GENERAL) }

            LaunchedEffect(generalWindowState) {
                currentScreen = generalWindowState
                openedFromGeneral = generalWindowState == GeneralWindowState.GENERAL
            }

            WindowDraggableArea {
                AppTheme {
                    when (currentScreen) {
                        GeneralWindowState.GENERAL -> ClipboardScreen(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    0.1.dp,
                                    color = AppTheme.colors.outline.copy(alpha = 0.2f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                )
                                .background(AppTheme.colors.background),
                            onClose = closeWindow,
                            onOpenFullApp = { currentScreen = GeneralWindowState.SETTING },
//                            headerContent = {
////                                ClipboardSectionHeader(
////                                    title = "Home",
////                                    description = "All copied data is available in one place."
////                                )
//                            }
                        )

                        GeneralWindowState.SETTING -> SettingScreen(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    0.1.dp,
                                    color = AppTheme.colors.outline.copy(alpha = 0.2f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                )
                                .background(AppTheme.colors.background),
                            onBack = {
                                if (openedFromGeneral) {
                                    currentScreen = GeneralWindowState.GENERAL
                                } else {
                                    closeWindow()
                                }
                            },
                            onOpenConnection = {}
                        )

                        GeneralWindowState.CLOSE -> Unit
                    }
                }
            }
        }
    }
}
