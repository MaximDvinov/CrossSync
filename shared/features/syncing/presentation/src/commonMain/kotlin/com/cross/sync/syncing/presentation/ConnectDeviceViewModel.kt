package com.cross.sync.syncing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.repository.LocalClipboardRepository
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
    val sendErrorMessage: String? = null,
    val isSendErrorVisible: Boolean = false,
    val connectionErrorMessage: String? = null,
    val isConnectionErrorVisible: Boolean = false
)

class ConnectDeviceViewModel(
    private val pairToServerUseCase: PairToServerUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val sendCopiedDataUseCase: SendCopiedDataUseCase,
    private val getCopiedDataUseCase: GetCopiedDataUseCase,
    private val localClipboardRepository: LocalClipboardRepository
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
                        updateConnectionState(connectState)
                    }
                }
                .onFailure { exception ->
                    if (exception is DeviceNotInitializedException) {
                        updateConnectionState(ClientConnectState.Idle)
                        return@onFailure
                    }

                    updateConnectionState(ClientConnectState.Error(exception.toHumanMessage()))
                }
        }
    }

    fun pair(deviceId: String, deviceName: String, qrCode: String) {
        viewModelScope.launch(Dispatchers.Main) {
            if (_state.value.connectState != ClientConnectState.Connecting) {
                updateConnectionState(ClientConnectState.Connecting)
                pairToServerUseCase(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    qrConnectionConfig = qrCode
                ).onSuccess {
                    println("StartConnect")
                    connectToServerUseCase().onSuccess {
                        it.collect { connectState ->
                            updateConnectionState(connectState)
                        }
                    }.onFailure { exception ->
                        updateConnectionState(ClientConnectState.Error(exception.toHumanMessage()))
                    }
                }.onFailure { exception ->
                    updateConnectionState(ClientConnectState.Error(exception.toHumanMessage()))
                }
            }
        }
    }

    fun sendCopiedData(copiedData: CopiedData) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSendingCopiedData = true,
                    sendErrorMessage = null,
                    isSendErrorVisible = false
                )
            }

            localClipboardRepository.addCopiedData(copiedData)
            sendCopiedDataUseCase(copiedData)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSendingCopiedData = false,
                            sendErrorMessage = null,
                            isSendErrorVisible = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isSendingCopiedData = false,
                            sendErrorMessage = exception.message ?: "Не удалось отправить данные",
                            isSendErrorVisible = true
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
                    it.copy(
                        sendErrorMessage = "Буфер обмена устройства пуст",
                        isSendErrorVisible = true
                    )
                }
                return@launch
            }

            sendCopiedData(currentClipboardData)
        }
    }

    fun dismissSendError() {
        _state.update {
            it.copy(isSendErrorVisible = false)
        }
    }

    fun dismissConnectionError() {
        _state.update {
            it.copy(isConnectionErrorVisible = false)
        }
    }

    fun onLocalNetworkPermissionDenied() {
        updateConnectionState(
            ClientConnectState.Error(
                "Для синхронизации с Mac нужен доступ к локальной сети"
            )
        )
    }

    private fun updateConnectionState(connectState: ClientConnectState) {
        val errorMessage = when (connectState) {
            is ClientConnectState.Disconnected -> {
                val reason = connectState.cause?.message ?: "Не удалось подключиться к серверу"
                "Ошибка подключения: $reason"
            }

            is ClientConnectState.Error -> "Ошибка подключения: ${connectState.message}"
            else -> null
        }

        _state.update {
            it.copy(
                connectState = connectState,
                connectionErrorMessage = errorMessage,
                isConnectionErrorVisible = errorMessage != null
            )
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
