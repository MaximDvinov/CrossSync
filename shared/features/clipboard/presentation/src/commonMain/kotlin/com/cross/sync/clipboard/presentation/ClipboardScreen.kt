@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    onClose: (() -> Unit)? = null,
    onOpenFullApp: (() -> Unit)? = null,
    onPaste: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isLargeControls: Boolean = true,
    maxVisibleItems: Int? = null,
    headerContent: (@Composable () -> Unit)? = null,
    categoryActions: @Composable RowScope.() -> Unit = {},
    showCategoryBar: Boolean = true,
    showClearAllButton: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    val dragAndDropState = rememberDragAndDropState<CopiedDataStable>()
    val contentHorizontalPadding = if (isLargeControls) 20.dp else 10.dp
    val topInsetPadding = contentPadding.calculateTopPadding()
    val bottomInsetPadding = contentPadding.calculateBottomPadding()
    val listBottomPadding = (if (isLargeControls) 20.dp else 10.dp) + bottomInsetPadding

    AppTheme {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            TopBar(
                onClose = onClose,
                onOpenSettings = onOpenFullApp,
                actionButtonSize = if (isLargeControls) 40.dp else 28.dp,
                logoSize = if (isLargeControls) 40.dp else 28.dp,
                contentPadding = contentHorizontalPadding,
                topPadding = 10.dp + topInsetPadding
            )
            headerContent?.invoke()

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
                    if (showCategoryBar) {
                        CategoryBar(
                            modifier = Modifier.padding(bottom = 10.dp),
                            categories = state.categories,
                            selectedCategory = state.selectedCategory,
                            clearAll = {
                                viewModel.clearAll()
                            },
                            onSelect = viewModel::selectCategory,
                            addCopiedDataToCategory = viewModel::addCopiedDataToCategory,
                            dragAndDropState = dragAndDropState,
                            isLargeControls = isLargeControls,
                            categoryActions = categoryActions,
                            showClearAllButton = showClearAllButton,
                            contentHorizontalPadding = contentHorizontalPadding
                        )
                    }

                    CopiedDataList(
                        copiedDataList = state.copiedDataList,
                        onPaste = onPaste ?: {},
                        currentCategory = state.selectedCategory,
                        onAddCopiedData = viewModel::addCopiedData,
                        onDelete = viewModel::deleteCopiedData,
                        currentCopiedData = state.currentCopiedData,
                        dragAndDropState = dragAndDropState,
                        maxVisibleItems = maxVisibleItems,
                        contentHorizontalPadding = contentHorizontalPadding,
                        bottomPadding = listBottomPadding
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
    isLargeControls: Boolean,
    categoryActions: @Composable RowScope.() -> Unit,
    showClearAllButton: Boolean,
    contentHorizontalPadding: Dp
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = contentHorizontalPadding)
        ) {
            item {
                CategoryItem(
                    category = CategoryStable.ALL,
                    onSelect = { onSelect(null) },
                    isSelect = selectedCategory == null,
                    isLargeControls = isLargeControls
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
                    category.id == selectedCategory?.id,
                    isLargeControls = isLargeControls
                )
            }
            item {
                CategoryItem(
                    category = CategoryStable.UNCATEGORIZED,
                    onSelect = { onSelect(CategoryStable.UNCATEGORIZED) },
                    isSelect = selectedCategory?.id == CategoryStable.UNCATEGORIZED.id,
                    isLargeControls = isLargeControls
                )
            }
        }

        categoryActions()

        if (showClearAllButton) {
            RoundedTextButton(
                modifier = Modifier.padding(end = contentHorizontalPadding),
                onClick = clearAll,
                colors = if (isLargeControls) {
                    ButtonsDefaults.buttonPadding(horizontal = 16.dp, vertical = 11.dp)
                } else {
                    ButtonsDefaults.buttonPadding(vertical = 6.dp)
                },
                text = "Clear All",
                textStyle = if (isLargeControls) {
                    AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurfaceVariant)
                } else {
                    AppTheme.typography.semiBold12.copy(color = AppTheme.colors.onSurfaceVariant)
                },
            )
        }
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
    maxVisibleItems: Int? = null,
    contentHorizontalPadding: Dp,
    bottomPadding: Dp,
) {
    val listState = rememberLazyListState()
    val displayedCopiedDataList = if (maxVisibleItems != null && maxVisibleItems > 0) {
        copiedDataList.take(maxVisibleItems)
    } else {
        copiedDataList
    }

    LaunchedEffect(currentCopiedData) {
        if (displayedCopiedDataList.isNotEmpty()) {
            val index = displayedCopiedDataList.indexOfFirst { it.id == currentCopiedData?.id }
            listState.animateScrollToItem(if (index >= 0) index else 0)
        }

    }

    LaunchedEffect(currentCategory) {
        delay(200)
        if (displayedCopiedDataList.isNotEmpty()) {
            val index = displayedCopiedDataList.indexOfFirst { it.id == currentCopiedData?.id }
            listState.scrollToItem(if (index >= 0) index else 0)
        }
    }

    if (displayedCopiedDataList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentHorizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "Clipboard history is empty",
                style = AppTheme.typography.medium14.copy(color = AppTheme.colors.outline)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier,
            state = listState,
            contentPadding = PaddingValues(
                start = contentHorizontalPadding,
                end = contentHorizontalPadding,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayedCopiedDataList, key = { it.id }) { copiedData ->
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
}

@Composable
fun ClipboardSectionHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            text = title,
            style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
        )
        BasicText(
            text = description,
            style = AppTheme.typography.regular12.copy(color = AppTheme.colors.outline)
        )
    }
}
