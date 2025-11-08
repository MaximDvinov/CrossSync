package com.cross.sync.clipboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.presentation.ui.CopiedDataItem
import com.cross.sync.theme.AppTheme
import org.koin.compose.koinInject

@Suppress("NonSkippableComposable")
@Composable
fun ClipboardScreen(
    modifier: Modifier,
    viewModel: ClipboardViewModel = koinInject(),
    onPaste: () -> Unit,
) {
    val copiedDataListState by viewModel.copiedDataListFlow.collectAsState()
    val currentCopiedDataListState by viewModel.copiedDataFlow.collectAsState()

    AppTheme {
        Column(
            modifier = modifier.fillMaxSize().background(AppTheme.colors.background)
        ) {
            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(10.dp),
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