package com.cross.sync.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

abstract class BaseScreen(
    val metaData: Map<String, Any> = emptyMap(),
) : NavKey {
    @Composable
    abstract fun Content()
}