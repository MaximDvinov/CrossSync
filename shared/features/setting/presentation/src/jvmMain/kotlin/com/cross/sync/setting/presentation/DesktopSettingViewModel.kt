package com.cross.sync.setting.presentation

import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.usecase.AddCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.RenameCategoryUseCase
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.syncing.domain.usecases.AddDeviceUseCase
import com.cross.sync.syncing.domain.usecases.ObserveDevicesUseCase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DesktopSettingViewModel(
    private val observeCategoryUseCase: ObserveCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val renameCategoryUseCase: RenameCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val addDeviceUseCase: AddDeviceUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase
) : SettingViewModel() {
    init {
        viewModelScope.launch {
            observeCategoryUseCase().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }

        viewModelScope.launch {
            observeDevicesUseCase().collect { devices ->
                _state.update { it.copy(devices = devices) }
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            addCategoryUseCase.invoke(category = Category(0, name))
        }
    }

    fun renameCategory(categoryId: Long, name: String) {
        viewModelScope.launch {
            renameCategoryUseCase(Category(categoryId, name))
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            deleteCategoryUseCase(categoryId)
        }
    }

    fun pairingDevice() {
        viewModelScope.launch {
            addDeviceUseCase().collect { state ->
                _state.update {
                    it.copy(pairingState = state)
                }
            }
        }
    }

    fun qrCodeCancel() {
        _state.update {
            it.copy(pairingState = PairingState.Idle())
        }
    }
}







