package com.cross.sync.clipboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.presentation.ui.CopiedDataItem
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Close
import org.koin.compose.koinInject

@Suppress("NonSkippableComposable")
@Composable
fun ClipboardScreen(
    modifier: Modifier,
    viewModel: ClipboardViewModel = koinInject(),
    onClose: () -> Unit,
    onOpenFullApp: () -> Unit,
    onPaste: () -> Unit,
) {
    val copiedDataListState by viewModel.copiedDataListFlow.collectAsState()
    val currentCopiedDataListState by viewModel.copiedDataFlow.collectAsState()

    AppTheme {
        Column(
            modifier = modifier.fillMaxSize().background(AppTheme.colors.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    modifier = Modifier.weight(1f),
                    text = "CrossSync",
                    style = AppTheme.typography.semiBold20.copy(AppTheme.colors.primary)
                )

                Box(
                    Modifier
                        .clickable(onClick = onClose)
                        .size(30.dp)
                        .clip(AppTheme.shapes.round10)
                        .background(AppTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppTheme.icons.Close,
                        contentDescription = null,
                        tint = AppTheme.colors.onSurfaceVariant
                    )
                }
            }
            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(copiedDataListState) { copiedData ->
                    CopiedDataItem(
                        modifier = Modifier.fillMaxWidth().pointerInput(copiedData) {
                            awaitPointerEventScope {
                                var lastClickTime = 0L
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastClickTime < 250) {
                                            viewModel.addCopiedData(copiedData)
                                            onPaste()
                                        }
                                        lastClickTime = currentTime
                                    }
                                }
                            }
                        },
                        copiedData = copiedData,
                        isSelected = copiedData == currentCopiedDataListState,
                        onClick = {
                            viewModel.addCopiedData(copiedData)
                        }
                    )
                }
            }
        }
    }
}