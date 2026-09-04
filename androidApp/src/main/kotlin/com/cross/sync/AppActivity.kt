package com.cross.sync

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cross.sync.client.MainScreen
import com.cross.sync.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Napier.base(DebugAntilog())

        setContent {
            ThemeChanged(true)

            AppTheme {
                LocalNetworkPermissionGate { onPairRequested ->
                    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                    MainScreen(
                        modifier = Modifier
                            .background(AppTheme.colors.background),
                        isClient = true,
                        contentPadding = systemBarsPadding,
                        onPairRequested = onPairRequested
                    )
                }
            }

        }
    }
}

@Composable
private fun LocalNetworkPermissionGate(
    content: @Composable (onPairRequested: ((() -> Unit), () -> Unit) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var pendingPairing by remember { mutableStateOf<PendingPairing?>(null) }
    var initialRequestResolved by remember {
        mutableStateOf(!requiresLocalNetworkPermission(context))
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingPairing?.let { pairing ->
            if (isGranted) pairing.onGranted() else pairing.onDenied()
            pendingPairing = null
        }
        initialRequestResolved = true
    }

    LaunchedEffect(initialRequestResolved) {
        if (!initialRequestResolved) {
            launcher.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
        }
    }

    if (initialRequestResolved) {
        content { onGranted, onDenied ->
            if (requiresLocalNetworkPermission(context)) {
                pendingPairing = PendingPairing(onGranted, onDenied)
                launcher.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
            } else {
                onGranted()
            }
        }
    }
}

private fun requiresLocalNetworkPermission(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
        context,
        ACCESS_LOCAL_NETWORK_PERMISSION
    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
}

private data class PendingPairing(
    val onGranted: () -> Unit,
    val onDenied: () -> Unit
)

private const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
private fun ThemeChanged(isLight: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isLight) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}
