package com.cross.sync.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cross.sync.clipboard.presentation.ClipboardScreen
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedButton
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.CrossSync

@Composable
fun ClientClipboardScreen(
    modifier: Modifier,
    onOpenFullApp: (() -> Unit)? = null,
    onSendToMac: (() -> Unit)? = null,
    isSendAvailable: Boolean,
    isSendingToMac: Boolean = false,
    sendErrorMessage: String? = null,
    isSendErrorVisible: Boolean = false,
    onDismissSendError: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        ClipboardScreen(
            modifier = Modifier.fillMaxSize(),
            onOpenFullApp = onOpenFullApp,
            isLargeControls = true,
            contentPadding = contentPadding,
        )

        if (isSendAvailable && onSendToMac != null) {
            RoundedButton(
                onClick = onSendToMac,
                enabled = !isSendingToMac,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = 24.dp + contentPadding.calculateBottomPadding()
                    )
                    .size(64.dp)
                    .dropShadow(shape = RoundedCornerShape(20.dp)) {
                        offset = Offset(0f, 10f)
                        radius = 16f
                        alpha = 0.2f
                    },
                colors = ButtonsDefaults.buttonPadding(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                    shape = RoundedCornerShape(20.dp)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppTheme.icons.CrossSync,
                        contentDescription = "Sync clipboard",
                        tint = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isSendingToMac) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = AppTheme.colors.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }

        if (isSendErrorVisible && !sendErrorMessage.isNullOrBlank()) {
            ErrorPopupDialog(
                message = sendErrorMessage,
                onDismiss = onDismissSendError
            )
        }
    }
}

@Composable
private fun ErrorPopupDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.background,
                    shape = AppTheme.shapes.round12
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BasicText(
                text = "Ошибка",
                style = AppTheme.typography.semiBold16.copy(color = AppTheme.colors.primary)
            )
            BasicText(
                text = message,
                style = AppTheme.typography.medium14.copy(color = AppTheme.colors.onSurface)
            )
            RoundedTextButton(
                onClick = onDismiss,
                text = "OK",
                colors = ButtonsDefaults.buttonPadding(horizontal = 16.dp, vertical = 10.dp),
                textStyle = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
