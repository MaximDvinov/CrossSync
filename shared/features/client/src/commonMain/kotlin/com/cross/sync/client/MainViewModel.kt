package com.cross.sync.client

import androidx.lifecycle.ViewModel
import com.cross.sync.syncing.domain.usecases.ConnectToServerUseCase

class MainViewModel(
    private val connectToServerUseCase: ConnectToServerUseCase
): ViewModel() {
    
}