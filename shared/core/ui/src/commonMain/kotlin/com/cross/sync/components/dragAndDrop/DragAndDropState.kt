package com.cross.sync.components.dragAndDrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.AbstractCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope

@Stable
data class DragAndDropData<T>(
    val id: String,
    val data: T
)

@Stable
class DragAndDropState<T>(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _globalPosition: MutableState<Offset> = mutableStateOf(Offset.Zero)
    private val _state: MutableState<DragAndDropState> = mutableStateOf(DragAndDropState.IDLE)
    private val _draggableData: MutableState<DragAndDropData<T>?> = mutableStateOf(null)

    var globalPosition: State<Offset> = _globalPosition
    val state: State<DragAndDropState> = _state
    val draggableData: State<DragAndDropData<T>?> = _draggableData

    internal fun updatePosition(position: Offset) {
        _globalPosition.value = position
    }

    internal fun updateState(state: DragAndDropState) {
        _state.value = state
    }

    internal fun onDragStart(startPosition: Offset, data: DragAndDropData<T>) {
        _globalPosition.value = startPosition
        _draggableData.value = data

        _state.value = DragAndDropState.PROGRESS
    }

    internal fun onDragEnd() {
        _state.value = DragAndDropState.DROP
        coroutineScope.launch {
            delay(300)
            _state.value = DragAndDropState.IDLE
            _globalPosition.value = Offset.Zero
            _draggableData.value = null
        }
    }

    @Stable
    enum class DragAndDropState {
        IDLE, PROGRESS, DROP
    }
}

@Composable
fun <T> rememberDragAndDropState(): DragAndDropState<T> {
    val coroutineScope = rememberCoroutineScope()
    return remember { DragAndDropState(coroutineScope) }
}