package com.cross.sync

import android.content.ClipData
import android.content.Context
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

class AndroidClipboardManager(
    private val context: Context
) : ClipboardManager {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    private val state = MutableStateFlow<CopiedData?>(null)
    private var isInitialized = false
    private var skipNextClipboardCallback = false
    private var suppressClipboardEchoUntilMillis: Long = 0L
    private var suppressClipboardEchoText: String? = null

    private val clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        if (skipNextClipboardCallback) {
            skipNextClipboardCallback = false
            return@OnPrimaryClipChangedListener
        }
        val clipboardData = readClipboardData()
        if (clipboardData is CopiedData.Text && shouldSuppressEcho(clipboardData.text)) {
            return@OnPrimaryClipChangedListener
        }
        state.value = clipboardData
    }

    override fun observeData(): StateFlow<CopiedData?> {
        return state
    }

    override suspend fun setData(data: CopiedData) {
        skipNextClipboardCallback = true
        val text = when (data) {
            is CopiedData.Text -> data.text
            is CopiedData.FormattedText -> data.plainText
            is CopiedData.Image -> data.imagePath
            is CopiedData.File -> data.filePaths.joinToString("\n")
        }

        clipboardManager.setPrimaryClip(
            ClipData.newPlainText("CrossSync", text)
        )

        registerClipboardEchoSuppression(text)
        state.value = data
    }

    override suspend fun getData(): CopiedData? {
        val data = readClipboardData()
        state.value = data
        return data
    }

    override fun init(): StateFlow<CopiedData?> {
        if (!isInitialized) {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            isInitialized = true
        }
        state.value = readClipboardData()
        return state
    }

    private fun readClipboardData(): CopiedData? {
        val primaryClip = clipboardManager.primaryClip ?: return null
        if (primaryClip.itemCount <= 0) return null

        val item = primaryClip.getItemAt(0)
        val text = item.text?.toString()?.takeIf { it.isNotBlank() }
            ?: item.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
            ?: return null

        return CopiedData.Text(
            id = text.hashCode().toLong(),
            text = text,
            dateTime = Clock.System.now(),
            applicationId = "android.system"
        )
    }

    private fun registerClipboardEchoSuppression(text: String?) {
        suppressClipboardEchoText = text?.trim()?.takeIf { it.isNotEmpty() }
        suppressClipboardEchoUntilMillis = System.currentTimeMillis() + CLIPBOARD_ECHO_SUPPRESSION_MS
    }

    private fun shouldSuppressEcho(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.isEmpty()) return false
        val suppressionText = suppressClipboardEchoText ?: return false
        if (System.currentTimeMillis() > suppressClipboardEchoUntilMillis) return false
        return normalized == suppressionText
    }

    private companion object {
        private const val CLIPBOARD_ECHO_SUPPRESSION_MS = 2500L
    }
}
