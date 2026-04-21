package com.cross.sync.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close

@Composable
actual fun SettingScreen(
    modifier: Modifier,
    viewModel: SettingViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background)
            .padding(horizontal = 12.dp)
    ) {
        TopBar(
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            text = "Settings",
            rightAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.Close,
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                )
            }
        )

        BasicText(
            text = "Подключение к Mac",
            style = AppTheme.typography.semiBold20.copy(color = AppTheme.colors.primary),
            modifier = Modifier.padding(top = 8.dp)
        )

        BasicText(
            text = "Откройте экран подключения, отсканируйте QR-код с Mac и дождитесь статуса соединения.",
            style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        )

        RoundedTextButton(
            onClick = onOpenConnection,
            text = "Open Connection Screen",
            colors = com.cross.sync.components.button.ButtonsDefaults.buttonPadding(
                horizontal = 18.dp,
                vertical = 10.dp,
                shape = AppTheme.shapes.round12
            ),
            textStyle = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant)
        )
    }
}
