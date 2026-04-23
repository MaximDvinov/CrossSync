@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataToCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ClearCopiedDataByIdUseCase
import com.cross.sync.clipboard.domain.usecase.ClearUncategorizedCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCopiedDataByIdUseCase
import com.cross.sync.clipboard.domain.usecase.GetApplicationsUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataByCategoryUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCurrentCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveUncategorizedCopiedDataUseCase
import com.cross.sync.clipboard.presentation.model.CategoryStable
import com.cross.sync.clipboard.presentation.model.CopiedDataStable
import com.cross.sync.clipboard.presentation.model.toStable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.uuid.ExperimentalUuidApi

@Stable
data class ClipboardState(
    val copiedDataList: List<CopiedDataStable> = listOf(),
    val currentCopiedData: CopiedDataStable? = null,
    val categories: List<CategoryStable> = listOf(),
    val selectedCategory: CategoryStable? = null,
)

class ClipboardViewModel(
    private val addCopiedDataUseCase: AddCopiedDataUseCase,
    private val initClipboardManagerUseCase: InitClipboardManagerUseCase,
    private val deleteCopiedDataByIdUseCase: DeleteCopiedDataByIdUseCase,
    private val observeCopiedDataUseCase: ObserveCopiedDataUseCase,
    observeCurrentCopiedDataUseCase: ObserveCurrentCopiedDataUseCase,
    observeCategoryUseCase: ObserveCategoryUseCase,
    private val observeCopiedDataByCategoryUseCase: ObserveCopiedDataByCategoryUseCase,
    private val observeUncategorizedCopiedDataUseCase: ObserveUncategorizedCopiedDataUseCase,
    private val getApplicationsUseCase: GetApplicationsUseCase,
    private val clearCopiedDataByIdUseCase: ClearCopiedDataByIdUseCase,
    private val clearUncategorizedCopiedDataUseCase: ClearUncategorizedCopiedDataUseCase,
    private val addCopiedDataToCategory: AddCopiedDataToCategoryUseCase
) : ViewModel() {
    private val copiedDataFlow = observeCurrentCopiedDataUseCase().map { it?.toStable() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    private val categories =
        observeCategoryUseCase().map { it.map { category -> category.toStable() } }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = listOf()
        )

    private val _state: MutableStateFlow<ClipboardState> = MutableStateFlow(ClipboardState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            initClipboardManagerUseCase()
        }

        selectCategory()

        viewModelScope.launch {
            copiedDataFlow.collect {
                _state.value = _state.value.copy(currentCopiedData = it)
            }
        }

        viewModelScope.launch {
            categories.collect {
                _state.value = _state.value.copy(categories = it)
            }
        }
    }

    private var observeDataJob: Job? = null
    fun selectCategory(category: CategoryStable? = null) {
        observeDataJob?.cancel()
        observeDataJob = viewModelScope.launch {
            _state.update { it.copy(selectedCategory = category) }
            if (category == null) {
                observeCopiedDataUseCase().combine(getApplicationsUseCase()) { copiedData, applications ->
                    copiedData to applications
                }.collect { (copiedData, applications) ->
                    val combined = copiedData.map {
                        val app = applications.find { app -> app.id == it.applicationId }
                        it.toStable(app)
                    }
                    _state.value = _state.value.copy(copiedDataList = combined)
                }
            } else if (category.id == CategoryStable.UNCATEGORIZED.id) {
                observeUncategorizedCopiedDataUseCase().combine(getApplicationsUseCase()) { copiedData, applications ->
                    copiedData to applications
                }.collect { (copiedData, applications) ->
                    val combined = copiedData.map {
                        val app = applications.find { app -> app.id == it.applicationId }
                        it.toStable(app)
                    }
                    _state.value = _state.value.copy(copiedDataList = combined)
                }
            } else {
                observeCopiedDataByCategoryUseCase(category.id).combine(getApplicationsUseCase()) { copiedData, applications ->
                    copiedData to applications
                }.collect { (copiedData, applications) ->
                    val combined = copiedData.map {
                        val app = applications.find { app -> app.id == it.applicationId }
                        it.toStable(app)
                    }
                    _state.value = _state.value.copy(copiedDataList = combined)
                }
            }
        }
    }

    fun addCopiedData(data: CopiedDataStable) {
        viewModelScope.launch {
            addCopiedDataUseCase(data.id)
        }
    }

    fun addCopiedDataToCategory(categoryId: Long, data: CopiedDataStable) {
        viewModelScope.launch {
            println("$categoryId, ${data.id}")
            addCopiedDataToCategory(categoryId, data.id)
        }
    }


    fun deleteCopiedData(copiedData: CopiedDataStable) {
        viewModelScope.launch {
            deleteCopiedDataByIdUseCase(copiedData.id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val selectedCategory = _state.value.selectedCategory
            if (selectedCategory?.id == CategoryStable.UNCATEGORIZED.id) {
                clearUncategorizedCopiedDataUseCase()
            } else {
                clearCopiedDataByIdUseCase(selectedCategory?.id)
            }
        }
    }
}
