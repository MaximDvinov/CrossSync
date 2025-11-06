import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.FlavorEvent
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DesktopClipboardManager() : ClipboardManager {
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    val coroutineScope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun observeData(): Flow<CopiedData?> {
        println("Clipboard init")
        val flow: MutableStateFlow<CopiedData?> = MutableStateFlow(null)
        coroutineScope.launch {
            var lastData: CopiedData.Text = getData() as CopiedData.Text
            while (true) {
                delay(300)
                val currentData = getData() as CopiedData.Text
                if (currentData.text != lastData.text) {
                    lastData = currentData
                    flow.value = currentData
                }
            }

//            clipboard.addFlavorListener { e: FlavorEvent ->
//                try {
//                    val data =
//                        clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
//                    flow.value = data?.let {
//                        CopiedData.Text(
//                            id = Uuid.random(),
//                            text = it,
//                            date = Clock.System.now()
//                        )
//                    }
//
//                    println("Clipboard: $data")
//                } catch (ex: Exception) {
//                    println("Не удалось прочитать текст: ${ex.message}")
//                }
//            }
        }

        return flow
    }

    override suspend fun setData(data: CopiedData) {
        when (data) {
            is CopiedData.Text -> {
                val transferable: Transferable = StringSelection(data.text)

                clipboard.setContents(transferable) { _, _ ->
                    println("Буфер обмена изменился — мы больше не владелец данных.")
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun getData(): CopiedData? {
        val data =
            clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
        return data?.let {
            CopiedData.Text(
                id = Uuid.random(),
                text = it,
                date = Clock.System.now()
            )
        }
    }
}