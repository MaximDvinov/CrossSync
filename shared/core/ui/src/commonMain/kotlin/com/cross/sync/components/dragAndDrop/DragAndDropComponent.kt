package com.cross.sync.components.dragAndDrop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Offset.Companion.Zero
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize

@Composable
fun <T> Modifier.draggableComponent(
    dragAndDropState: DragAndDropState<T>,
    data: DragAndDropData<T>
): Modifier {
    var composableGlobalOffset by remember {
        mutableStateOf(Zero)
    }
    return this
        .onGloballyPositioned {
            composableGlobalOffset = it.positionInWindow()
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { localPointer ->
                    val globalPointer = composableGlobalOffset + localPointer
                    dragAndDropState.onDragStart(globalPointer, data)
                },
                onDragEnd = {
                    dragAndDropState.onDragEnd()
                }
            ) { change, _ ->
                val globalPointer = composableGlobalOffset + change.position
                dragAndDropState.updatePosition(globalPointer)
            }
        }
}

@Composable
fun <T> Modifier.dropTarget(
    dragAndDropState: DragAndDropState<T>,
    onStarted: () -> Unit = {},
    onEnded: () -> Unit = {},
    onDrop: (DragAndDropData<T>) -> Unit,
): Modifier {
    val dragGlobalPosition by dragAndDropState.globalPosition
    val state by dragAndDropState.state
    var dropPosition by remember {
        mutableStateOf(Zero)
    }
    var size by remember {
        mutableStateOf(Size.Zero)
    }

    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(state, dragGlobalPosition) {
        println("dropPosition=$dropPosition size=$size")
        println("dragGlobalPosition=$dragGlobalPosition")

        val dropRect = Rect(offset = dropPosition, size = size)

        if (dropRect.contains(dragGlobalPosition)) {
            if (state == DragAndDropState.DragAndDropState.DROP) {
                dragAndDropState.draggableData.value?.let(onDrop)
                onEnded()
            } else {
                if (!isStarted) {
                    isStarted = true
                    onStarted()
                }
            }
        } else {
            if (isStarted) {
                onEnded()
                isStarted = false
            }
        }
    }

    return this
        .onGloballyPositioned {
            dropPosition = it.positionInWindow()
        }
        .onSizeChanged {
            size = it.toSize()
        }
}

@Composable
fun <T> DragAndDropContainer(
    dragAndDropState: DragAndDropState<T>,
    modifier: Modifier,
    onDraggedContent: @Composable (DragAndDropData<T>) -> Unit,
    content: @Composable () -> Unit
) {
    val dragPosition by dragAndDropState.globalPosition
    val state by dragAndDropState.state
    val data by dragAndDropState.draggableData

    var offsetAfterScale by remember { mutableStateOf(Zero) }
    val scaleFactor = 0.3f
    val verticalPadding = 8f // отступ по Y, чтобы pointer не перекрывал контент

    var rootOffset by remember { mutableStateOf(Zero) }

    Box(modifier = modifier.onGloballyPositioned { rootOffset = it.positionInWindow() }) {
        content()

        AnimatedVisibility(
            visible = state == DragAndDropState.DragAndDropState.PROGRESS,
            modifier = Modifier.offset {
                IntOffset(
                    (dragPosition.x - rootOffset.x - offsetAfterScale.x).toInt(),
                    (dragPosition.y - rootOffset.y + verticalPadding - offsetAfterScale.y).toInt()
                )
            },
            enter = scaleIn(
                transformOrigin = TransformOrigin(0.5f, 0f)
            ),
            exit = scaleOut(
                transformOrigin = TransformOrigin(0.5f, 0f)
            )
        ) {
            data?.let {
                Box(
                    modifier = Modifier
                        .scale(scaleFactor)
                        .onSizeChanged { size ->
                            // Запоминаем половину размера контента с учётом scale
                            offsetAfterScale = Offset(
                                (size.width) / 2f,
                                (size.height) * scaleFactor
                            )
                        }
                ) {
                    onDraggedContent(it)
                }
            }
        }
    }
}








