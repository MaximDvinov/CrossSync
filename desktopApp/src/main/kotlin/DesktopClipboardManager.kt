@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Desktop clipboard manager with improved handling for formatted text and deduplication.
 */
class DesktopClipboardManager() : ClipboardManager {
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    // Map key: Int hash -> value: CopiedData
    private val savedCopiedData = ConcurrentHashMap<Int, CopiedData>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun observeData(): Flow<CopiedData?> {
        println("Clipboard init")
        val flow: MutableStateFlow<CopiedData?> = MutableStateFlow(null)
        coroutineScope.launch {
            var lastData: CopiedData? = getData()
            while (true) {
                delay(300)
                val currentData = try {
                    getData()
                } catch (e: Exception) {
                    null
                }

                if (currentData != null && currentData != lastData) {
                    println("New clipboard data: $currentData")
                    lastData = currentData
                    flow.value = currentData
                }
            }
        }

        return flow
    }

    override suspend fun setData(data: CopiedData) {
        when (data) {
            is CopiedData.Text -> {
                val transferable: Transferable = StringSelection(data.text)
                clipboard.setContents(transferable) { _, _ -> }
            }

            is CopiedData.FormattedText -> {
                // Provide multiple flavors: string, html as String and as InputStream (CF_HTML for Windows)
                val htmlStringFlavor = try { DataFlavor("${data.mimeType};class=java.lang.String") } catch (_: Exception) { null }
                val htmlStreamFlavor = try { DataFlavor("${data.mimeType};class=java.io.InputStream") } catch (_: Exception) { null }

                val flavorsList = mutableListOf<DataFlavor>().apply {
                    htmlStringFlavor?.let { add(it) }
                    htmlStreamFlavor?.let { add(it) }
                    add(DataFlavor.stringFlavor)
                }.toTypedArray()

                val transferable = object : Transferable {
                    override fun getTransferDataFlavors(): Array<DataFlavor> = flavorsList
                    override fun isDataFlavorSupported(f: DataFlavor): Boolean = flavorsList.any { it == f }

                    override fun getTransferData(f: DataFlavor): Any {
                        return when {
                            f == DataFlavor.stringFlavor -> htmlUnescape(stripHtmlToPlain(data.text))
                            htmlStringFlavor != null && f == htmlStringFlavor -> data.text
                            htmlStreamFlavor != null && f == htmlStreamFlavor -> {
                                // For InputStream provide UTF-8 bytes; for HTML also provide CF_HTML wrapper for Windows
                                val bytes = if (data.mimeType.equals("text/html", true)) {
                                    val cf = buildCfHtml(data.text)
                                    cf.toByteArray(Charsets.UTF_8)
                                } else {
                                    data.text.toByteArray(Charsets.UTF_8)
                                }
                                ByteArrayInputStream(bytes)
                            }
                            else -> throw UnsupportedFlavorException(f)
                        }
                    }
                }
                clipboard.setContents(transferable) { _, _ -> }
            }

            is CopiedData.Image -> {
                val image = loadImageFromFile(data.imagePath)
                println("Image loaded from file: $image")
                if (image != null) {
                    val transferable: Transferable = ImageSelection(image)
                    clipboard.setContents(transferable) { _, _ -> }
                }
            }

            is CopiedData.File -> {
                // Convert stored paths to java.io.File and put on clipboard as file list
                val files = data.filePaths.map { File(it) }
                val transferable: Transferable = FileSelection(files)
                clipboard.setContents(transferable) { _, _ -> }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun getData(): CopiedData? {
        return try {
            var result: CopiedData? = null
            SwingUtilities.invokeAndWait {
                try {
                    val contents = clipboard.getContents(null)
                    if (contents != null) {
                        // 1) Files
                        result = getFilesData(contents)
                        // 2) Image
                        if (result == null) result = getImageData(contents)
                        // 3) Formatted text (HTML/RTF)
                        if (result == null) result = getFormattedTextData(contents)
                        // 4) Plain text fallback
                        if (result == null) result = getPlainTextData(contents)
                    }
                } catch (e: Exception) {
                    println("Clipboard read error: ${e.message}")
                }
            }
            result
        } catch (e: Exception) {
            println("Clipboard exception: ${e.message}")
            null
        }
    }

    // --- Extracted helper methods for getData() ---
    private fun getFilesData(contents: Transferable): CopiedData.File? {
        return try {
            if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val list = try {
                    (contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                        ?.filterIsInstance<File>()
                } catch (e: Exception) {
                    null
                }
                if (!list.isNullOrEmpty()) {
                    val paths = list.map { it.absolutePath }
                    val hash = computeFilesHash(paths, list)
                    val existing = savedCopiedData[hash]
                    if (existing == null) {
                        val fileData = CopiedData.File(
                            filePaths = paths,
                            id = Uuid.random(),
                            date = Clock.System.now()
                        )
                        savedCopiedData[hash] = fileData
                        fileData
                    } else {
                        existing as? CopiedData.File
                    }
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getImageData(contents: Transferable): CopiedData.Image? {
        return try {
            if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                val image = try {
                    contents.getTransferData(DataFlavor.imageFlavor) as? Image
                } catch (e: Exception) {
                    null
                }
                if (image != null) {
                    val hash = imageHash(image)
                    if (savedCopiedData[hash] == null) {
                        val cacheDir = File(System.getProperty("user.home"), ".crosssync/images")
                        val fileName = "${hash}.png"
                        val savedFile = saveImage(image, cacheDir, fileName)
                        println("Saved image at: ${savedFile?.absolutePath}, hash: $hash")
                        savedFile?.absolutePath?.let {
                            val imgData = CopiedData.Image(
                                id = Uuid.random(),
                                imagePath = it,
                                date = Clock.System.now()
                            )
                            savedCopiedData[hash] = imgData
                            imgData
                        }
                    } else {
                        savedCopiedData[hash] as? CopiedData.Image
                    }
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getFormattedTextData(contents: Transferable): CopiedData.FormattedText? {
        return try {
            val htmlStringFlavor = try { DataFlavor("text/html;class=java.lang.String") } catch (_: Exception) { null }
            val htmlStreamFlavor = try { DataFlavor("text/html;class=java.io.InputStream") } catch (_: Exception) { null }
            val rtfStreamFlavor = try { DataFlavor("text/rtf;class=java.io.InputStream") } catch (_: Exception) { null }

            var formattedText: String? = null
            var mimeType: String? = null

            if (htmlStringFlavor != null && contents.isDataFlavorSupported(htmlStringFlavor)) {
                formattedText = try { contents.getTransferData(htmlStringFlavor) as? String } catch (_: Exception) { null }
                mimeType = "text/html"
            } else if (htmlStreamFlavor != null && contents.isDataFlavorSupported(htmlStreamFlavor)) {
                val stream = try { contents.getTransferData(htmlStreamFlavor) as? InputStream } catch (_: Exception) { null }
                formattedText = stream?.bufferedReader()?.use { it.readText() }
                mimeType = "text/html"
            } else if (rtfStreamFlavor != null && contents.isDataFlavorSupported(rtfStreamFlavor)) {
                val stream = try { contents.getTransferData(rtfStreamFlavor) as? InputStream } catch (_: Exception) { null }
                formattedText = stream?.bufferedReader()?.use { it.readText() }
                mimeType = "text/rtf"
            }

            if (!formattedText.isNullOrEmpty() && mimeType != null) {
                val plainNormalized = htmlUnescape(stripHtmlToPlain(formattedText))
                val hash = computeStringHash(plainNormalized, mimeType)
                val existing = savedCopiedData[hash]
                if (existing == null) {
                    val data = CopiedData.FormattedText(
                        id = Uuid.random(),
                        text = formattedText,
                        mimeType = mimeType,
                        date = Clock.System.now()
                    )
                    // deduplicate by content: if there's already same text with different mime, reuse
                    val found = savedCopiedData.values.firstOrNull {
                        when (it) {
                            is CopiedData.FormattedText -> htmlUnescape(stripHtmlToPlain(it.text)) == plainNormalized
                            is CopiedData.Text -> it.text == plainNormalized
                            else -> false
                        }
                    }
                    if (found != null) {
                        // use existing instance and map new hash to it
                        savedCopiedData[hash] = found
                        found as CopiedData.FormattedText
                    } else {
                        savedCopiedData[hash] = data
                        data
                    }
                } else {
                    existing as? CopiedData.FormattedText
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getPlainTextData(contents: Transferable): CopiedData.Text? {
        return try {
            val text = try { clipboard.getData(DataFlavor.stringFlavor) as? String } catch (e: Exception) { null }
            if (text.isNullOrEmpty()) return null
            val hash = computeStringHash(text, "text/plain")
            val existing = savedCopiedData[hash]
            if (existing == null) {
                // prefer reusing a FormattedText that contains same plain text
                val found = savedCopiedData.values.firstOrNull {
                    (it is CopiedData.FormattedText && stripHtmlToPlain(it.text) == stripHtmlToPlain(text)) ||
                            (it is CopiedData.Text && it.text == text)
                }
                if (found != null) {
                    savedCopiedData[hash] = found
                    return found as? CopiedData.Text
                }

                val textData = CopiedData.Text(
                    id = Uuid.random(),
                    text = text,
                    date = Clock.System.now()
                )
                savedCopiedData[hash] = textData
                textData
            } else {
                existing as? CopiedData.Text
            }
        } catch (e: Exception) {
            null
        }
    }

    // Transferable implementations for image and file-list
    private class ImageSelection(private val image: Image) : Transferable {
        private val flavors = arrayOf(DataFlavor.imageFlavor)
        override fun getTransferDataFlavors(): Array<DataFlavor> = flavors
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            DataFlavor.imageFlavor == flavor

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return image
        }
    }

    private class FileSelection(private val files: List<File>) : Transferable {
        private val flavors = arrayOf(DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor)

        override fun getTransferDataFlavors(): Array<DataFlavor> = flavors

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return flavors.any { it == flavor }
        }

        override fun getTransferData(flavor: DataFlavor): Any {
            return when {
                DataFlavor.javaFileListFlavor == flavor -> files
                DataFlavor.stringFlavor == flavor -> files.joinToString(separator = "\n") { it.absolutePath }
                else -> throw UnsupportedFlavorException(flavor)
            }
        }
    }
}


// --- helpers ---

fun toBufferedImage(img: Image): BufferedImage {
    if (img is BufferedImage) return img

    val bimage = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)
    val g = bimage.createGraphics()
    g.drawImage(img, 0, 0, null)
    g.dispose()
    return bimage
}

fun saveImage(image: Image, directory: File, fileName: String): File? {
    return try {
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        val bufferedImage = toBufferedImage(image)
        ImageIO.write(bufferedImage, "png", file) // Можно "jpg" или другой формат
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadImageFromFile(filePath: String): Image? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        ImageIO.read(file) // возвращает BufferedImage, совместим с Image
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Compute deterministic int-hash for an image.
 * Uses SHA-256 over ARGB pixels and folds digest into Int.
 */
fun imageHash(image: Image): Int {
    val buffered = if (image is BufferedImage) image else {
        val w = image.getWidth(null)
        val h = image.getHeight(null)
        if (w <= 0 || h <= 0) return 0
        val b = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = b.createGraphics()
        g.drawImage(image, 0, 0, null)
        g.dispose()
        b
    }

    // Получаем пиксели сразу в массив — быстрее и детерминированно
    val pixels = IntArray(buffered.width * buffered.height)
    buffered.getRGB(0, 0, buffered.width, buffered.height, pixels, 0, buffered.width)

    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(4)
    for (rgb in pixels) {
        buffer[0] = (rgb ushr 24).toByte()
        buffer[1] = (rgb ushr 16).toByte()
        buffer[2] = (rgb ushr 8).toByte()
        buffer[3] = rgb.toByte()
        digest.update(buffer)
    }

    return foldDigestToInt(digest.digest())
}

/**
 * Compute a hash for files list using path + size + lastModified for each file.
 * Efficient: no full-file reads.
 */
fun computeFilesHash(paths: List<String>, files: List<File>): Int {
    val md = MessageDigest.getInstance("SHA-256")
    for ((idx, p) in paths.withIndex()) {
        val f = files.getOrNull(idx)
        val meta = StringBuilder().apply {
            append(p)
            if (f != null && f.exists()) {
                append("|")
                append(f.length())
                append("|")
                append(f.lastModified())
            }
            append(";")
        }.toString()
        md.update(meta.toByteArray(Charsets.UTF_8))
    }
    return foldDigestToInt(md.digest())
}

fun computeStringHash(text: String, mimeType: String): Int {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(mimeType.toByteArray(Charsets.UTF_8))
    md.update('|'.code.toByte())
    md.update(text.toByteArray(Charsets.UTF_8))
    return foldDigestToInt(md.digest())
}

private fun foldDigestToInt(digest: ByteArray): Int {
    // simple deterministic fold: multiply-accumulate over unsigned bytes
    var result = 0
    for (b in digest) {
        result = result * 31 + (b.toInt() and 0xFF)
    }
    return result
}

private fun stripHtmlToPlain(html: String): String {
    // Lightweight fallback: remove tags and normalize whitespace. Enough for dedup checks.
    return html.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
}

/** Build a simple CF_HTML wrapper required by some Windows apps (StartHTML/EndHTML offsets). */
@Suppress("DefaultLocale")
private fun buildCfHtml(html: String): String {
    val utf8 = html.toByteArray(Charsets.UTF_8)
    val header = StringBuilder()
    header.append("Version:1.0\r\n")
    header.append("StartHTML:00000000\r\n")
    header.append("EndHTML:00000000\r\n")
    header.append("StartFragment:00000000\r\n")
    header.append("EndFragment:00000000\r\n")
    val pre = header.toString()
    val startFragmentMarker = "<!--StartFragment-->"
    val endFragmentMarker = "<!--EndFragment-->"
    val full = StringBuilder(pre)
    full.append(startFragmentMarker)
    full.append(html)
    full.append(endFragmentMarker)
    val bytes = full.toString().toByteArray(Charsets.UTF_8)

    val startHtml = pre.length
    val endHtml = bytes.size
    val startFragment = pre.length + startFragmentMarker.length
    val endFragment = startFragment + utf8.size

    val headerWithOffsets = pre.replaceRange(0, pre.length, String.format(
        "Version:1.0\r\nStartHTML:%08d\r\nEndHTML:%08d\r\nStartFragment:%08d\r\nEndFragment:%08d\r\n",
        startHtml, endHtml, startFragment, endFragment
    ))

    return headerWithOffsets + startFragmentMarker + html + endFragmentMarker
}

// Helper to decode basic HTML entities (for deduplication and plain text normalization)
private fun htmlUnescape(input: String): String {
    if (input.isEmpty()) return input
    val sb = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val c = input[i]
        if (c == '&') {
            val semicolon = input.indexOf(';', i + 1)
            if (semicolon > i) {
                val entity = input.substring(i + 1, semicolon)
                when {
                    entity.equals("lt", true) -> { sb.append('<'); i = semicolon + 1; continue }
                    entity.equals("gt", true) -> { sb.append('>'); i = semicolon + 1; continue }
                    entity.equals("amp", true) -> { sb.append('&'); i = semicolon + 1; continue }
                    entity.equals("quot", true) -> { sb.append('"'); i = semicolon + 1; continue }
                    entity.equals("apos", true) -> { sb.append('\''); i = semicolon + 1; continue }
                    entity.startsWith("#x") || entity.startsWith("#X") -> {
                        try {
                            val code = entity.substring(2).toInt(16)
                            sb.append(code.toChar())
                            i = semicolon + 1
                            continue
                        } catch (_: Exception) {}
                    }
                    entity.startsWith("#") -> {
                        try {
                            val code = entity.substring(1).toInt()
                            sb.append(code.toChar())
                            i = semicolon + 1
                            continue
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}
