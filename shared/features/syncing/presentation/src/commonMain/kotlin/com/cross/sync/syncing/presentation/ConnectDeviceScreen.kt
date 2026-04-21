package com.cross.sync.syncing.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cross.sync.syncing.domain.entity.ClientConnectState
import org.koin.compose.koinInject
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun ConnectDeviceScreen(
    modifier: Modifier = Modifier,
    connectDeviceViewModel: ConnectDeviceViewModel = koinInject(),
    content: @Composable (ConnectDeviceState) -> Unit
) {
    val state by connectDeviceViewModel.state.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (state.connectState) {
            ClientConnectState.Connected -> {
                content(state)
            }

            ClientConnectState.Connecting -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is ClientConnectState.Disconnected -> {
                BasicText(
                    "Disconnected \n ${(state.connectState as ClientConnectState.Disconnected).cause?.message}",
                    Modifier.align(Alignment.Center)
                )
            }

            ClientConnectState.Idle -> {
                ScannerWithPermissions(
                    onScanned = {
                        connectDeviceViewModel.pair(
                            deviceId = "1",
                            deviceName = "Pixel 9 Pro",
                            qrCode = it
                        )
                        true
                    },
                    types = listOf(CodeType.QR),
                    cameraPosition = CameraPosition.BACK,
                    enableTorch = false
                )
            }

            is ClientConnectState.Error -> {
                BasicText(
                    "Connect Error: \n ${(state.connectState as ClientConnectState.Error).message}",
                    Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
