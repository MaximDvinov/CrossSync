package com.cross.sync.setting.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.syncing.domain.entity.DeviceData
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Plus
import com.cross.sync.theme.icons.Trash
import qrgenerator.qrkitpainter.PatternType
import qrgenerator.qrkitpainter.QrBallType
import qrgenerator.qrkitpainter.QrFrameType
import qrgenerator.qrkitpainter.QrKitBrush
import qrgenerator.qrkitpainter.QrKitColors
import qrgenerator.qrkitpainter.QrKitShapes
import qrgenerator.qrkitpainter.QrPixelType
import qrgenerator.qrkitpainter.getSelectedFrameShape
import qrgenerator.qrkitpainter.getSelectedPattern
import qrgenerator.qrkitpainter.getSelectedPixel
import qrgenerator.qrkitpainter.getSelectedQrBall
import qrgenerator.qrkitpainter.rememberQrKitPainter
import qrgenerator.qrkitpainter.solidBrush

@Composable
fun DeviceSetting(
    state: SettingState, onCancel: () -> Unit, onDelete: () -> Unit, onAddDevice: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "Device Sync",
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
        )



        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(color = AppTheme.colors.surface).padding(10.dp)
        ) {
            BasicText(
                text = "Manage all settings related to syncing with external devices, including server configuration, adding new devices, and viewing currently connected ones.",
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.devices.forEach { device ->
                    DeviceItem(modifier = Modifier, device = device) {}
                }

                RoundedIconButton(
                    onClick = {
                        onAddDevice()
                    },
                    modifier = Modifier,
                    colors = ButtonsDefaults.buttonPadding(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                        shape = RoundedCornerShape(
                            topStart = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp
                        )
                    ),
                    imageVector = AppTheme.icons.Plus
                )
            }
        }

        if (state.pairingState !is PairingState.Idle) {
            Dialog(onDismissRequest = { onCancel() }) {
//            val centerLogo = painterResource(AppTheme.icons.CrossSync)

                Column(
                    modifier = Modifier
                        .width(230.dp)
                        .background(
                            AppTheme.colors.surface,
                            shape = AppTheme.shapes.round20
                        ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AnimatedContent(state, contentKey = {
                        when (state.pairingState) {
                            is PairingState.Connected -> 1
                            is PairingState.Error -> 2
                            is PairingState.Idle -> 3
                            is PairingState.QrCodeGenerated -> 4
                        }
                    }) {
                        when (state.pairingState) {
                            is PairingState.QrCodeGenerated -> {
                                QrCodeGenerated(state.pairingState, onAddDevice)
                            }

                            is PairingState.Connected -> {
                                Connected(state.pairingState, onCancel)
                            }

                            is PairingState.Error -> {}
                            is PairingState.Idle -> {

                            }
                        }
                    }
                }


            }
        }
    }
}

@Composable
private fun Connected(state: PairingState.Connected, onClose: () -> Unit) {
    Column(
        modifier = Modifier.width(230.dp)
            .padding(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = "Device added",
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.medium12Center.copy(color = AppTheme.colors.onSurface)
        )

        BasicText(
            text = state.deviceData.name,
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.bold16Center.copy(color = AppTheme.colors.onSurface)
        )

        RoundedTextButton(
            onClick = onClose,
            text = "Good!",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QrCodeGenerated(
    state: PairingState.QrCodeGenerated,
    onAddDevice: () -> Unit
) {
    val color = AppTheme.colors.primary
//    val painterLogo = painterLogo()
    val painter = rememberQrKitPainter(state.qrCode) {
        shapes = QrKitShapes(
            ballShape = getSelectedQrBall(QrBallType.RoundCornersQrBall(0.2f)),
            darkPixelShape = getSelectedPixel(QrPixelType.RoundCornerPixel()),
            frameShape = getSelectedFrameShape(QrFrameType.RoundCornersFrame(0.2f)),
            codeShape = getSelectedPattern(PatternType.SquarePattern),
        )
        colors = QrKitColors(
            darkBrush = QrKitBrush.solidBrush(color)
        )
//        logo = QrKitLogo(painter = painterLogo, padding = QrKitLogoPadding.Natural(0.0f))

    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        BasicText(
            text = "Scan this QR code\nusing the CrossSync app:",
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.bold12Center.copy(color = AppTheme.colors.onSurface)
        )

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.aspectRatio(1f).fillMaxWidth()
        )

        RoundedTextButton(
            onClick = { onAddDevice() },
            text = "Refresh",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DeviceItem(
    modifier: Modifier,
    device: DeviceData,
    onDelete: () -> Unit,
) {
    Row(modifier = modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = device.name,
            style = AppTheme.typography.medium14.copy(AppTheme.colors.onSurfaceVariant),
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 4.dp,
                        topEnd = 4.dp
                    )
                )
                .background(AppTheme.colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        RoundedIconButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonsDefaults.buttonPadding(
                horizontal = 8.dp,
                vertical = 4.dp,
                shape = RoundedCornerShape(
                    bottomStart = 4.dp,
                    topStart = 4.dp,
                    bottomEnd = 8.dp,
                    topEnd = 8.dp
                ),
                containerColor = AppTheme.colors.redContainer,
                contentColor = AppTheme.colors.onRedContainer
            ),
            imageVector = AppTheme.icons.Trash
        )
    }
}





