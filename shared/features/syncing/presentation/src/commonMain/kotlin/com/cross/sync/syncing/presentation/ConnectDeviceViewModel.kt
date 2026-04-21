package com.cross.sync.syncing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.usecase.GetCopiedDataUseCase
import com.cross.sync.syncing.domain.entity.ClientConnectState
import com.cross.sync.syncing.domain.entity.DeviceNotInitializedException
import com.cross.sync.syncing.domain.usecases.ConnectToServerUseCase
import com.cross.sync.syncing.domain.usecases.PairToServerUseCase
import com.cross.sync.syncing.domain.usecases.SendCopiedDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectDeviceState(
    val connectState: ClientConnectState,
    val isSendingCopiedData: Boolean = false,
    val sendError: String? = null
)

class ConnectDeviceViewModel(
    private val pairToServerUseCase: PairToServerUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val sendCopiedDataUseCase: SendCopiedDataUseCase,
    private val getCopiedDataUseCase: GetCopiedDataUseCase
) : ViewModel() {
    private var _state = MutableStateFlow(ConnectDeviceState(ClientConnectState.Idle))
    val state = _state.asStateFlow()

    init {
        autoConnectIfPaired()
    }

    private fun autoConnectIfPaired() {
        viewModelScope.launch(Dispatchers.Main) {
            connectToServerUseCase()
                .onSuccess { flow ->
                    flow.collect { connectState ->
                        _state.update { it.copy(connectState = connectState) }
                    }
                }
                .onFailure { exception ->
                    if (exception is DeviceNotInitializedException) {
                        _state.update { it.copy(connectState = ClientConnectState.Idle) }
                        return@onFailure
                    }

                    _state.update {
                        it.copy(connectState = ClientConnectState.Error(exception.toHumanMessage()))
                    }
                }
        }
    }

    fun pair(deviceId: String, deviceName: String, qrCode: String) {
        viewModelScope.launch(Dispatchers.Main) {
            if (_state.value.connectState != ClientConnectState.Connecting) {
                _state.update { it.copy(connectState = ClientConnectState.Connecting) }
                pairToServerUseCase(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    qrConnectionConfig = qrCode
                ).onSuccess {
                    println("StartConnect")
                    connectToServerUseCase().onSuccess {
                        it.collect { connectState ->
                            _state.update { it.copy(connectState = connectState) }
                        }
                    }.onFailure { exception ->
                        _state.update {
                            it.copy(
                                connectState = ClientConnectState.Error(exception.toHumanMessage())
                            )
                        }
                    }
                }.onFailure { exception ->
                    _state.update {
                        it.copy(
                            connectState = ClientConnectState.Error(exception.toHumanMessage())
                        )
                    }
                }
            }
        }
    }

    fun sendCopiedData(copiedData: CopiedData) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSendingCopiedData = true,
                    sendError = null
                )
            }

            sendCopiedDataUseCase(copiedData)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSendingCopiedData = false,
                            sendError = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isSendingCopiedData = false,
                            sendError = exception.message ?: "Не удалось отправить данные на Mac"
                        )
                    }
                }
        }
    }

    fun sendCurrentClipboardData() {
        viewModelScope.launch {
            val currentClipboardData = getCopiedDataUseCase()
            if (currentClipboardData == null) {
                _state.update {
                    it.copy(sendError = "Буфер обмена телефона пуст")
                }
                return@launch
            }

            sendCopiedData(currentClipboardData)
        }
    }

    private fun Throwable.toHumanMessage(): String {
        return when {
            message?.contains("Unexpected JSON") == true -> "QR-код некорректный"
            message.isNullOrBlank() -> "Не удалось подключиться к серверу"
            else -> message!!
        }
    }
}
