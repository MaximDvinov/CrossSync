package com.cross.sync.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.compose.viewmodel.koinViewModel

/** Clears the model and its subscriptions when this screen or popup leaves composition. */
@Composable
inline fun <reified T : ViewModel> rememberScreenViewModel(): T {
    // Tray content can live outside a Window and has no host-provided owner.
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    return koinViewModel(viewModelStoreOwner = owner)
}
