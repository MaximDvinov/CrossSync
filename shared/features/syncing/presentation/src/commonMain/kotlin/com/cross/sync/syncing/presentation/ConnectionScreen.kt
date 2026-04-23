package com.cross.sync.syncing.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.window.Dialog
import com.cross.sync.components.TopBar
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit
) {
    val state by connectDeviceViewModel.state.collectAsState()
    val (statusText, isError) = connectionStatusText(state.connectState)
    val activeError = state.connectionErrorMessage
        ?.takeIf { state.isConnectionErrorVisible }

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
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp + contentPadding.calculateTopPadding(),
                    bottom = 12.dp
                ),
            text = "Connection",
            rightAction = {
                RoundedIconButton(
                    imageVector = AppTheme.icons.Close,
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                )
            }
        )

        AnimatedVisibility(
            visible = statusText.isNotBlank() && !isError,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 20.dp + contentPadding.calculateBottomPadding()
                ),
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

        if (activeError != null) {
            Dialog(onDismissRequest = connectDeviceViewModel::dismissConnectionError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.background)
                        .padding(16.dp)
                ) {
                    Column {
                        BasicText(
                            text = "Ошибка подключения",
                            style = AppTheme.typography.semiBold16.copy(color = AppTheme.colors.primary)
                        )
                        BasicText(
                            text = activeError,
                            style = AppTheme.typography.medium14.copy(color = AppTheme.colors.onSurface),
                            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
                        )
                        RoundedTextButton(
                            onClick = connectDeviceViewModel::dismissConnectionError,
                            text = "OK",
                            colors = ButtonsDefaults.buttonPadding(horizontal = 18.dp, vertical = 10.dp),
                            textStyle = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
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
