import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.di.clipboardModule
import com.cross.sync.clipboard.domain.entity.Application
import com.cross.sync.clipboard.domain.usecase.SaveApplicationsUseCase
import com.cross.sync.setting.di.settingModule
import com.cross.sync.syncing.di.syncingModule
import com.tulskiy.keymaster.common.Provider
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import utils.getInstalledApplications
import windows.GeneralWindow
import windows.GeneralWindowState
import windows.QuickClipboardWindow
import kotlin.time.ExperimentalTime

val desktopModule = module {
    singleOf(::DesktopClipboardManager) bind ClipboardManager::class
    single<Provider> { Provider.getCurrentProvider(true) }
    singleOf(::GlobalHotkeyManager)
}

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication({
        modules(desktopModule, syncingModule, clipboardModule, settingModule)
    }) {
//        val startSyncUseCase = koinInject<StartSyncUseCase>()
        val saveApplicationsUseCase = koinInject<SaveApplicationsUseCase>()
        val globalHotkeyManager = koinInject<GlobalHotkeyManager>()

        LaunchedEffect(Unit) {
            launch {
                val applications = getInstalledApplications() + Application(
                    id = "com.apple.finder",
                    name = "Finder",
                    null,
                    ""
                )
                saveApplicationsUseCase(applications)
            }
        }

//        LaunchedEffect(Unit) {
//            startSyncUseCase()
//        }

        var generalWindowShowed by remember {
            mutableStateOf<GeneralWindowState?>(null)
        }

        QuickClipboardWindow(
            openSetting = {
                generalWindowShowed = GeneralWindowState.SETTING
            },
            openHome = {
                generalWindowShowed = GeneralWindowState.GENERAL
            },
            globalHotkeyManager = globalHotkeyManager
        )
        generalWindowShowed?.let {
            GeneralWindow(it) {
                generalWindowShowed = null
            }
        }
    }
}

