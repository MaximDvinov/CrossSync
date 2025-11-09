@file:OptIn(ExperimentalUuidApi::class)

package com.cross.sync.clipboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.clipboard.domain.usecase.AddCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.InitClipboardManagerUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCopiedDataUseCase
import com.cross.sync.clipboard.domain.usecase.ObserveCurrentCopiedDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

class ClipboardViewModel(
    private val addCopiedDataUseCase: AddCopiedDataUseCase,
    private val initClipboardManagerUseCase: InitClipboardManagerUseCase,
    observeCopiedDataUseCase: ObserveCopiedDataUseCase,
    observeCurrentCopiedDataUseCase: ObserveCurrentCopiedDataUseCase,
) : ViewModel() {
    val copiedDataListFlow = observeCopiedDataUseCase().map {
        it.map { it.toStable() }.reversed()
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
}