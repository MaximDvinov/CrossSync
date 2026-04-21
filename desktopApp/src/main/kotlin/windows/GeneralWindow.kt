package windows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
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
            WindowDraggableArea {
                AppTheme {
                    SettingScreen(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                0.1.dp,
                                color = Color(0x3300253C),
                                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            )
                            .background(AppTheme.colors.background),
                        onBack = closeWindow,
                        onOpenConnection = {}
                    )
                }
            }
        }
    }
}
