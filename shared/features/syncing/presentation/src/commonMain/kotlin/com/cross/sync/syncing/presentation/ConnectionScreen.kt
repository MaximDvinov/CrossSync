package com.cross.sync.syncing.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import org.koin.compose.koinInject
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    connectDeviceViewModel: ConnectDeviceViewModel = koinInject(),
    onBack: () -> Unit
) {
    val state by connectDeviceViewModel.state.collectAsState()
    val (statusText, isError) = connectionStatusText(state.connectState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        ScannerWithPermissions(
            onScanned = {
                connectDeviceViewModel.pair(
                    deviceId = "android-client",
                    deviceName = "Android",
                    qrCode = it
                )
                false
            },
            types = listOf(CodeType.QR),
            cameraPosition = CameraPosition.BACK,
            enableTorch = false
        )

        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            text = "Connection",
            rightAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.Close,
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                )
            }
        )

        AnimatedVisibility(
            visible = statusText.isNotBlank(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            enter = slideInVertically(animationSpec = tween(280)) { fullHeight -> fullHeight / 2 } + fadeIn(
                animationSpec = tween(220)
            ),
            exit = slideOutVertically(animationSpec = tween(240)) { fullHeight -> fullHeight / 2 } + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isError) AppTheme.colors.redContainer else AppTheme.colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = statusText,
                    style = AppTheme.typography.medium14.copy(
                        color = if (isError) AppTheme.colors.onRedContainer else AppTheme.colors.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun connectionStatusText(state: ClientConnectState): Pair<String, Boolean> {
    return when (state) {
        ClientConnectState.Idle -> "Отсканируйте QR-код с Mac для подключения" to false
        ClientConnectState.Connecting -> "Подключение к серверу..." to false
        ClientConnectState.Connected -> "Устройство успешно подключено к Mac" to false
        is ClientConnectState.Disconnected -> {
            val reason = state.cause?.message ?: "Не удалось подключиться к серверу"
            "Ошибка подключения: $reason" to true
        }

        is ClientConnectState.Error -> "Ошибка подключения: ${state.message}" to true
    }
}
