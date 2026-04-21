package com.cross.sync.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

class NavController(
    val navBackStack: NavBackStack<NavKey>,
) {
    fun push(screen: NavKey) {
        navBackStack.add(screen)
    }

    fun pop(): Boolean {
        return navBackStack.removeLastOrNull() != null
    }

    fun popTo(destination: NavKey, inclusive: Boolean = false) {
        val index = navBackStack.indexOf(destination)
        if (index < 0) return

        val removeFromIndex = if (inclusive) index else index + 1
        if (removeFromIndex > navBackStack.lastIndex) return

        val entriesToRemove = navBackStack
            .subList(removeFromIndex, navBackStack.size)
            .toList()
        navBackStack.removeAll(entriesToRemove)
    }

    fun clearBackStack() {
        navBackStack.clear()
    }

    fun getCurrentScreen(): NavKey? {
        return navBackStack.lastOrNull()
    }
}

@Composable
fun rememberNavController(
    startDestination: NavKey,
): NavController {
    val navBackStack = remember(startDestination) { NavBackStack(startDestination) }

    return remember(Unit) {
        NavController(navBackStack)
    }
}

val LocalNavController =
    compositionLocalOf<NavController> { error("NavController invalid") }

@Composable
fun NavHost(
    startScreen: BaseScreen,
    modifier: Modifier,
) {
    val navController = rememberNavController(startScreen)

    CompositionLocalProvider(
        LocalNavController provides navController
    ) {
        NavDisplay(
            backStack = navController.navBackStack,
            modifier = modifier,
        ) { key ->
            NavEntry(key) {}
        }
    }
}
