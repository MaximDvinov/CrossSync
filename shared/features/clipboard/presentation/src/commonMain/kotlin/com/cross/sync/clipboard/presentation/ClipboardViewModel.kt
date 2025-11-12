@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.DeleteCopiedDataByIdUseCase
import com.cross.sync.clipboard.domain.usecase.GetApplicationsUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCurrentCopiedDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

class ClipboardViewModel(
    private val addCopiedDataUseCase: AddCopiedDataUseCase,
    private val initClipboardManagerUseCase: InitClipboardManagerUseCase,
    private val deleteCopiedDataByIdUseCase: DeleteCopiedDataByIdUseCase,
    observeCopiedDataUseCase: ObserveCopiedDataUseCase,
    observeCurrentCopiedDataUseCase: ObserveCurrentCopiedDataUseCase,
    getApplicationsUseCase: GetApplicationsUseCase,
) : ViewModel() {
    val copiedDataListFlow =
        observeCopiedDataUseCase().combine(getApplicationsUseCase()) { copiedData, applications ->
            copiedData.map {
                val app = applications.find { app -> app.id == it.applicationId }
                println(app)
                it.toStable(app)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = listOf()
        )

    val copiedDataFlow = observeCurrentCopiedDataUseCase().map { it?.toStable() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    init {
        viewModelScope.launch {
            initClipboardManagerUseCase()
        }
    }

    fun addCopiedData(data: CopiedDataStable) {
        viewModelScope.launch {
            addCopiedDataUseCase(data.id)
        }
    }

    fun deleteCopiedData(copiedData: CopiedDataStable) {
        viewModelScope.launch {
            deleteCopiedDataByIdUseCase(copiedData.id)
        }
    }
}