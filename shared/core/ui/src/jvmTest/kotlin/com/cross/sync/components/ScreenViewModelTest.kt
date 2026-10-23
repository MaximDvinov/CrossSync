package com.cross.sync.components

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {
    @Test
    fun repeatedPopupDisposalCancelsDatabaseSubscriptions() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val notifications = MutableStateFlow(0)
        val models = module { viewModel { ObservingViewModel(notifications) } }
        try {
            repeat(50) {
                val recomposer = Recomposer(coroutineContext)
                val composition = Composition(EmptyApplier(), recomposer)
                try {
                    composition.setContent {
                        KoinApplication(application = { modules(models) }) {
                            rememberScreenViewModel<ObservingViewModel>()
                        }
                    }
                    runCurrent()
                    assertEquals(1, notifications.subscriptionCount.value)
                } finally {
                    composition.dispose()
                    recomposer.cancel()
                }
                runCurrent()
                assertEquals(0, notifications.subscriptionCount.value)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    class ObservingViewModel(notifications: MutableStateFlow<Int>) : ViewModel() {
        init {
            viewModelScope.launch { notifications.collect {} }
        }
    }

    private class EmptyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
