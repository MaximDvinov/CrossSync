package com.cross.sync.setting.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.clipboard.domain.usecase.AddCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.RenameCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class SettingState(
    val categories: List<Category> = listOf()
)

class SettingViewModel(
    private val observeCategoryUseCase: ObserveCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val renameCategoryUseCase: RenameCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeCategoryUseCase().collect { categories ->
                _state.update { it.copy(categories = categories) }
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

}







