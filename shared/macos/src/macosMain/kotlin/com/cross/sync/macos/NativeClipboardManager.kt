package com.cross.sync.macos

import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import com.cross.sync.setting.domain.SettingPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class NativeClipboardManager(
    private val desktop: DesktopServices,
    private val preferences: SettingPreferencesStore,
    private val scope: CoroutineScope,
) : ClipboardManager {
    private val data = MutableStateFlow<CopiedData?>(null)
    private var monitor: Job? = null
    private var revision: Int? = null
    override fun init(): StateFlow<CopiedData?> {
        if (monitor == null) monitor = scope.launch(Dispatchers.Main) {
            while (true) {
                val nextRevision = desktop.clipboardRevision()
                if (revision != nextRevision) {
                    revision = nextRevision
                    val content = desktop.readClipboard()
                    if (content != null && !preferences.isApplicationExcluded(content.applicationId.orEmpty())) {
                        data.value = content.toDomain()
                    }
                }
                delay(750)
            }
        }
        return data
    }
    override fun observeData(): StateFlow<CopiedData?> = data
    override suspend fun getData(): CopiedData? = withContext(Dispatchers.Main) {
        desktop.readClipboard()?.toDomain()
    }
    override suspend fun setData(data: CopiedData) = withContext(Dispatchers.Main) {
        desktop.writeClipboard(data.toNative())?.let { error(it) }
        revision = desktop.clipboardRevision()
        this@NativeClipboardManager.data.value = data
    }
}

internal fun NativeClipboardContent.toDomain(): CopiedData {
    val id = (kind + text + paths.joinToString("\u0000")).hashCode().toLong()
    return when (kind) {
        "html", "rtf" -> CopiedData.FormattedText(id, text, "text/$kind", plainText, applicationId = applicationId)
        "image" -> CopiedData.Image(id, paths.first(), applicationId = applicationId)
        "files" -> CopiedData.File(id, paths, applicationId = applicationId)
        else -> CopiedData.Text(id, text, applicationId = applicationId)
    }
}

internal fun CopiedData.toNative(): NativeClipboardContent = when (this) {
    is CopiedData.Text -> NativeClipboardContent("text", text, text, emptyList(), applicationId)
    is CopiedData.FormattedText -> NativeClipboardContent(mimeType.substringAfter('/'), text, plainText, emptyList(), applicationId)
    is CopiedData.Image -> NativeClipboardContent("image", "", "", listOf(imagePath), applicationId)
    is CopiedData.File -> NativeClipboardContent("files", "", "", filePaths, applicationId)
}
