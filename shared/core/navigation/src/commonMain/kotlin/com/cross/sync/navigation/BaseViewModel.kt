package com.cross.sync.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface Action
interface Intent
interface State
interface Event

abstract class BaseViewModel<STATE : State, ACTION : Action, INTENT : Intent, EVENT : Event>(
    private val initialState: STATE,
) : ViewModel() {
    private val mutableStateFlow: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    val state: StateFlow<STATE> by lazy {
        mutableStateFlow
            .onStart {
                viewModelScope.launch {
                    bootstrap()
                }
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Lazily, initialState)
    }

    private val _event: Channel<EVENT> = Channel(Channel.BUFFERED)
    val event: Flow<EVENT>
        get() = _event.receiveAsFlow()

    fun onAction(action: ACTION) {
        viewModelScope.launch {
            onIntent(actionToIntent(action))
        }
    }

    protected fun onIntent(intent: INTENT) {
        viewModelScope.launch {
            val currentState = mutableStateFlow.value
            val (newState, event) = reduce(intent, currentState)

            mutableStateFlow.update { newState }
            event?.let { _event.send(it) }
        }
    }

    protected abstract suspend fun actionToIntent(action: ACTION): INTENT

    protected abstract suspend fun reduce(
        intent: INTENT,
        oldState: STATE,
    ): Pair<STATE, EVENT?>

    protected abstract suspend fun bootstrap()
}