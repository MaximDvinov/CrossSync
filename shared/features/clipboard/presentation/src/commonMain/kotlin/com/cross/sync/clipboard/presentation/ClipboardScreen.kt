@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.presentation.model.CategoryStable
import com.cross.sync.clipboard.presentation.model.CopiedDataStable
import com.cross.sync.clipboard.presentation.ui.CategoryItem
import com.cross.sync.clipboard.presentation.ui.CopiedDataItem
import com.cross.sync.clipboard.presentation.ui.TopBar
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedTextButton
import com.cross.sync.components.dragAndDrop.DragAndDropContainer
import com.cross.sync.components.dragAndDrop.DragAndDropData
import com.cross.sync.components.dragAndDrop.DragAndDropState
import com.cross.sync.components.dragAndDrop.draggableComponent
import com.cross.sync.components.dragAndDrop.dropTarget
import com.cross.sync.components.dragAndDrop.rememberDragAndDropState
import com.cross.sync.theme.AppTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi

@Suppress("NonSkippableComposable")
@Composable
fun ClipboardScreen(
    modifier: Modifier,
    viewModel: ClipboardViewModel = koinInject(),
    onClose: () -> Unit,
    onOpenFullApp: () -> Unit,
    onPaste: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dragAndDropState = rememberDragAndDropState<CopiedDataStable>()

    AppTheme {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            TopBar(onClose)

            DragAndDropContainer(
                dragAndDropState = dragAndDropState,
                modifier = Modifier.fillMaxSize(),
                onDraggedContent = { data ->
                    CopiedDataItem(
                        modifier = Modifier.fillMaxWidth(),
                        copiedData = data.data,
                        isShowTag = false
                    )
//                    Box(Modifier.size(10.dp).background(Color.Red, shape = CircleShape))
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    CategoryBar(
                        modifier = Modifier.padding(bottom = 10.dp),
                        categories = state.categories,
                        selectedCategory = state.selectedCategory,
                        clearAll = {
                            viewModel.clearAll()
                        },
                        onSelect = viewModel::selectCategory,
                        addCopiedDataToCategory = viewModel::addCopiedDataToCategory,
                        dragAndDropState = dragAndDropState
                    )

                    CopiedDataList(
                        copiedDataList = state.copiedDataList,
                        onPaste = onPaste,
                        currentCategory = state.selectedCategory,
                        onAddCopiedData = viewModel::addCopiedData,
                        onDelete = viewModel::deleteCopiedData,
                        currentCopiedData = state.currentCopiedData,
                        dragAndDropState = dragAndDropState
                    )
                }

            }

        }
    }
}

@Composable
fun CategoryBar(
    modifier: Modifier,
    categories: List<CategoryStable>,
    selectedCategory: CategoryStable?,
    clearAll: () -> Unit,
    onSelect: (CategoryStable?) -> Unit,
    addCopiedDataToCategory: (Long, CopiedDataStable) -> Unit,
    dragAndDropState: DragAndDropState<CopiedDataStable>,
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            item {
                CategoryItem(
                    category = CategoryStable.ALL,
                    onSelect = { onSelect(null) },
                    isSelect = selectedCategory == null,
                )
            }
            items(categories, key = { it.id }) { category ->
                var dragged by remember {
                    mutableStateOf(false)
                }

                CategoryItem(
                    modifier = Modifier
                        .background(
                            if (dragged)
                                AppTheme.colors.surfaceVariant.copy(0.5f)
                            else Color.Transparent
                        )
                        .dropTarget(
                            dragAndDropState,
                            onStarted = { dragged = true },
                            onEnded = { dragged = false },
                            onDrop = { addCopiedDataToCategory(category.id, it.data) },
                        ),
                    category = category,
                    onSelect = {
                        onSelect(category)
                    },
                    category.id == selectedCategory?.id
                )
            }
        }

        RoundedTextButton(
            modifier = Modifier.padding(end = 10.dp),
            onClick = clearAll,
            colors = ButtonsDefaults.buttonPadding(vertical = 6.dp),
            text = "Clear All",
            textStyle = AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
fun CopiedDataList(
    copiedDataList: List<CopiedDataStable>,
    onPaste: () -> Unit,
    currentCategory: CategoryStable?,
    onAddCopiedData: (CopiedDataStable) -> Unit,
    onDelete: (CopiedDataStable) -> Unit,
    currentCopiedData: CopiedDataStable?,
    dragAndDropState: DragAndDropState<CopiedDataStable>,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentCopiedData) {
        val index = copiedDataList.indexOfFirst { it.id == currentCopiedData?.id }
        listState.animateScrollToItem(if (index >= 0) index else 0)

    }

    LaunchedEffect(currentCategory){
        delay(200)
        val index = copiedDataList.indexOfFirst { it.id == currentCopiedData?.id }
        listState.scrollToItem(if (index >= 0) index else 0)
    }

    LazyColumn(
        modifier = Modifier,
        state = listState,
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(copiedDataList, key = { it.id }) { copiedData ->
            CopiedDataItem(
                modifier = Modifier.fillMaxWidth()
                    .draggableComponent(
                        dragAndDropState,
                        DragAndDropData(copiedData.id.toString(), copiedData)
                    ),
                copiedData = copiedData,
                isSelected = copiedData == currentCopiedData,
                onClick = {
                    onAddCopiedData(copiedData)
                },
                onDoubleClick = {
                    onAddCopiedData(copiedData)
                    onPaste()
                },
                onDeleteClick = {
                    onDelete(copiedData)
                }
            )
        }
    }
}