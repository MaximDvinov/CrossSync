@file:OptIn( ExperimentalTime::class)

import androidx.compose.ui.util.fastJoinToString
import com.cross.sync.clipboard.data.ClipboardManager
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.getFrontmostAppBundleId
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
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.LinkedHashMap
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import javax.swing.text.BadLocationException
import javax.swing.text.rtf.RTFEditorKit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Desktop clipboard manager with improved handling for formatted text (HTML/RTF), images and files.
 * Includes memory optimizations: LRU cache, downscaled image hashing, adaptive polling.
 */
class DesktopClipboardManager() : ClipboardManager {
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    // Synchronized LRU cache for clipboard data
    private val savedCopiedDataLock = Any()
    private val savedCopiedData = object : LinkedHashMap<Int, CopiedData>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, CopiedData>?): Boolean {
            return size > 256
        }
    }

    private fun cachePut(hash: Int, value: CopiedData) {
        synchronized(savedCopiedDataLock) { savedCopiedData[hash] = value }
    }

    private fun cacheGet(hash: Int): CopiedData? =
        synchronized(savedCopiedDataLock) { savedCopiedData[hash] }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    val flow: MutableStateFlow<CopiedData?> = MutableStateFlow(null)

    override fun init(): StateFlow<CopiedData?> {
        coroutineScope.launch {
            var lastData: CopiedData? = getData()
            flow.update { lastData }
            var interval = 1000L
            while (true) {
                delay(interval)
                val currentData = try {
                    getData()
                } catch (e: Exception) {
                    null
                }

                if (currentData != null && currentData != lastData) {
                    println("New clipboard data: ${currentData.log()}")
                    lastData = currentData
                    flow.value = currentData
                    interval = 500L
                } else {
                    interval = minOf(5000L, interval + 200L)
                }
            }
        }
        return flow
    }

    @OptIn( ExperimentalTime::class)
    override fun observeData(): StateFlow<CopiedData?> {
        return flow
    }

    override suspend fun setData(data: CopiedData) {
        when (data) {
            is CopiedData.Text -> {
                val transferable: Transferable = StringSelection(data.text)
                clipboard.setContents(transferable) { _, _ -> }
            }

            is CopiedData.FormattedText -> {
                // Provide multiple flavors: string, html/rtf as String and as InputStream
                val mainStringFlavor = try {
                    DataFlavor("${data.mimeType};class=java.lang.String")
                } catch (_: Exception) {
                    null
                }
                val mainStreamFlavor = try {
                    DataFlavor("${data.mimeType};class=java.io.InputStream")
                } catch (_: Exception) {
                    null
                }
                val rtfStreamFlavor = try {
                    DataFlavor("text/rtf;class=java.io.InputStream")
                } catch (_: Exception) {
                    null
                }

                val flavorsList = mutableListOf<DataFlavor>().apply {
                    mainStringFlavor?.let { add(it) }
                    mainStreamFlavor?.let { add(it) }
                    rtfStreamFlavor?.let { if (data.mimeType.equals("text/rtf", true)) add(it) }
                    add(DataFlavor.stringFlavor)
                }.toTypedArray()

                val transferable = object : Transferable {
                    override fun getTransferDataFlavors(): Array<DataFlavor> = flavorsList
                    override fun isDataFlavorSupported(f: DataFlavor): Boolean =
                        flavorsList.any { it == f }

                    override fun getTransferData(f: DataFlavor): Any {
                        return when {
                            // plain text consumers
                            f == DataFlavor.stringFlavor -> {
                                if (data.mimeType.equals("text/rtf", true)) {
                                    data.plainText
                                } else if (data.mimeType.equals("text/html", true)) {
                                    htmlUnescape(stripHtmlToPlain(data.text))
                                } else {
                                    htmlUnescape(stripHtmlToPlain(data.text))
                                }
                            }

                            mainStringFlavor != null && f == mainStringFlavor -> data.text

                            mainStreamFlavor != null && f == mainStreamFlavor -> {
                                if (data.mimeType.equals("text/html", true)) {
                                    val cf = buildCfHtml(data.text)
                                    ByteArrayInputStream(cf.toByteArray(Charsets.UTF_8))
                                } else {
                                    ByteArrayInputStream(data.text.toByteArray(Charsets.UTF_8))
                                }
                            }

                            rtfStreamFlavor != null && f == rtfStreamFlavor -> {
                                ByteArrayInputStream(data.text.toByteArray(Charsets.ISO_8859_1))
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
                val files = data.filePaths.map { File(it) }
                val transferable: Transferable = FileSelection(files)
                clipboard.setContents(transferable) { _, _ -> }
            }
        }

        flow.update { data }
    }

    @OptIn(ExperimentalTime::class)
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
                    val existing = cacheGet(hash)
                    if (existing == null) {
                        val fileData = CopiedData.File(
                            filePaths = paths,
                            id = hash,
                            dateTime = Clock.System.now(),
                            applicationId = getFrontmostAppBundleId()
                        )
                        cachePut(hash, fileData)
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
                    val hash = imageHashDownscaled(image, 128)
                    if (cacheGet(hash) == null) {
                        val cacheDir = File(System.getProperty("user.home"), ".crosssync/images")
                        val fileName = "${hash}.png"
                        val savedFile = saveImage(image, cacheDir, fileName)
                        println("Saved image at: ${savedFile?.absolutePath}, hash: $hash")
                        savedFile?.absolutePath?.let {
                            val imgData = CopiedData.Image(
                                id = hash,
                                imagePath = it,
                                dateTime = Clock.System.now(),
                                applicationId = getFrontmostAppBundleId()
                            )
                            cachePut(hash, imgData)
                            imgData
                        }
                    } else {
                        cacheGet(hash) as? CopiedData.Image
                    }
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getFormattedTextData(contents: Transferable): CopiedData.FormattedText? {
        return try {
            val htmlStringFlavor = try {
                DataFlavor("text/html;class=java.lang.String")
            } catch (_: Exception) {
                null
            }
            val htmlStreamFlavor = try {
                DataFlavor("text/html;class=java.io.InputStream")
            } catch (_: Exception) {
                null
            }
            val rtfStreamFlavor = try {
                DataFlavor("text/rtf;class=java.io.InputStream")
            } catch (_: Exception) {
                null
            }

            var formattedRaw: String? = null
            var mimeType: String? = null
            var plainNormalized: String? = null

            if (htmlStringFlavor != null && contents.isDataFlavorSupported(htmlStringFlavor)) {
                val raw = try {
                    contents.getTransferData(htmlStringFlavor) as? String
                } catch (_: Exception) {
                    null
                }
                if (!raw.isNullOrEmpty()) {
                    formattedRaw = raw
                    plainNormalized = htmlUnescape(stripHtmlToPlain(raw))
                    mimeType = "text/html"
                }
            } else if (htmlStreamFlavor != null && contents.isDataFlavorSupported(htmlStreamFlavor)) {
                val stream = try {
                    contents.getTransferData(htmlStreamFlavor) as? InputStream
                } catch (_: Exception) {
                    null
                }
                if (stream != null) {
                    val bytes = stream.readBytes()
                    val raw = String(bytes, Charsets.UTF_8)
                    formattedRaw = raw
                    plainNormalized = htmlUnescape(stripHtmlToPlain(raw))
                    mimeType = "text/html"
                }
            } else if (rtfStreamFlavor != null && contents.isDataFlavorSupported(rtfStreamFlavor)) {
                val stream = try {
                    contents.getTransferData(rtfStreamFlavor) as? InputStream
                } catch (_: Exception) {
                    null
                }
                if (stream != null) {
                    // read raw bytes once
                    val bytes = stream.readBytes()
                    // try robust RTF parsing to plain
                    val plain = rtfStreamToPlain(ByteArrayInputStream(bytes))
                        ?: rtfFallbackToPlain(String(bytes, Charset.forName("windows-1251")))
                    // store raw as ISO-8859-1 string to keep round-trip to bytes
                    formattedRaw = String(bytes, Charsets.ISO_8859_1)
                    plainNormalized = plain
                    mimeType = "text/rtf"
                }
            }

            if (!formattedRaw.isNullOrEmpty() && mimeType != null && !plainNormalized.isNullOrEmpty()) {
                val hash = computeStringHash(plainNormalized, mimeType)
                val existing = cacheGet(hash)
                if (existing == null) {
                    val data = CopiedData.FormattedText(
                        id = hash,
                        text = formattedRaw,
                        mimeType = mimeType,
                        plainText = plainNormalized,
                        dateTime = Clock.System.now(),
                        applicationId = getFrontmostAppBundleId()
                    )
                    val found = synchronized(savedCopiedDataLock) {
                        savedCopiedData.values.firstOrNull {
                            when (it) {
                                is CopiedData.FormattedText -> it.plainText == plainNormalized
                                is CopiedData.Text -> it.text == plainNormalized
                                else -> false
                            }
                        }
                    }
                    if (found != null) {
                        cachePut(hash, found)
                        found as CopiedData.FormattedText
                    } else {
                        cachePut(hash, data)
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
            val text = try {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } catch (e: Exception) {
                null
            }
            if (text.isNullOrEmpty()) return null
            val hash = computeStringHash(text, "text/plain")
            val existing = cacheGet(hash)
            if (existing == null) {
                val found = synchronized(savedCopiedDataLock) {
                    savedCopiedData.values.firstOrNull {
                        (it is CopiedData.FormattedText && it.plainText == text) || (it is CopiedData.Text && it.text == text)
                    }
                }
                if (found != null) {
                    cachePut(hash, found)
                    return found as? CopiedData.Text
                }

                val textData = CopiedData.Text(
                    id = hash,
                    text = text,
                    dateTime = Clock.System.now(),
                    applicationId = getFrontmostAppBundleId()
                )
                cachePut(hash, textData)
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
        ImageIO.write(bufferedImage, "png", file)
        bufferedImage.flush()
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
        ImageIO.read(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Compute deterministic int-hash for an image.
 * Downscales image, hashes ARGB, folds digest into Int.
 */
fun imageHashDownscaled(image: Image, maxSize: Int = 128): Int {
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

    val scale = maxSize.toDouble() / maxOf(buffered.width, buffered.height)
    val newW = (buffered.width * scale).toInt().coerceAtLeast(1)
    val newH = (buffered.height * scale).toInt().coerceAtLeast(1)
    val resized = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
    val g = resized.createGraphics()
    g.drawImage(buffered, 0, 0, newW, newH, null)
    g.dispose()

    val pixels = IntArray(newW * newH)
    resized.getRGB(0, 0, newW, newH, pixels, 0, newW)
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(4)
    for (rgb in pixels) {
        buffer[0] = (rgb ushr 24).toByte()
        buffer[1] = (rgb ushr 16).toByte()
        buffer[2] = (rgb ushr 8).toByte()
        buffer[3] = rgb.toByte()
        digest.update(buffer)
    }

    resized.flush()
    buffered.flush()

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
    var result = 0
    for (b in digest) {
        result = result * 31 + (b.toInt() and 0xFF)
    }
    return result
}

private fun stripHtmlToPlain(html: String): String {
    return html.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
}

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

    val headerWithOffsets = pre.replaceRange(
        0, pre.length, String.format(
            "Version:1.0\r\nStartHTML:%08d\r\nEndHTML:%08d\r\nStartFragment:%08d\r\nEndFragment:%08d\r\n",
            startHtml, endHtml, startFragment, endFragment
        )
    )

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
                    entity.equals("lt", true) -> {
                        sb.append('<'); i = semicolon + 1; continue
                    }

                    entity.equals("gt", true) -> {
                        sb.append('>'); i = semicolon + 1; continue
                    }

                    entity.equals("amp", true) -> {
                        sb.append('&'); i = semicolon + 1; continue
                    }

                    entity.equals("quot", true) -> {
                        sb.append('\"'); i = semicolon + 1; continue
                    }

                    entity.equals("apos", true) -> {
                        sb.append('\''); i = semicolon + 1; continue
                    }

                    entity.startsWith("#x") || entity.startsWith("#X") -> {
                        try {
                            val code = entity.substring(2).toInt(16); sb.append(code.toChar()); i =
                                semicolon + 1; continue
                        } catch (_: Exception) {
                        }
                    }

                    entity.startsWith("#") -> {
                        try {
                            val code = entity.substring(1).toInt(); sb.append(code.toChar()); i =
                                semicolon + 1; continue
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

/** Parse RTF stream to plain text using Swing's RTFEditorKit. Returns null on failure. */
private fun rtfStreamToPlain(stream: InputStream): String? {
    return try {
        val kit = RTFEditorKit()
        val doc = kit.createDefaultDocument()
        val bytes = stream.readBytes()
        ByteArrayInputStream(bytes).use { bais -> kit.read(bais, doc, 0) }
        try {
            val text = doc.getText(0, doc.length)
            text.replace(Regex("\\s+"), " ").trim()
        } catch (ex: BadLocationException) {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/** Fallback RTF->plain: decode \uNNNN and hex escapes, remove tags. */
private fun rtfFallbackToPlain(raw: String): String {
    // decode unicode escapes \uNNNN
    val withUnicode = Regex("""\\u(-?\d+)""").replace(raw) { m ->
        try {
            val code = m.groupValues[1].toInt()
            val normalized = if (code < 0) code + 0x10000 else code
            normalized.toChar().toString()
        } catch (_: Exception) {
            ""
        }
    }
    // replace hex escapes \'hh using windows-1251
    val replacedHex = Regex("""\\'([0-9a-fA-F]{2})""").replace(withUnicode) { m ->
        val hex = m.groupValues[1]
        val b = hex.toInt(16).toByte()
        try {
            String(byteArrayOf(b), Charset.forName("windows-1251"))
        } catch (_: Exception) {
            ""
        }
    }
    var stripped = replacedHex.replace(Regex("""\\[a-zA-Z]+-?\d* ?"""), "")
    stripped = stripped.replace(Regex("""[\{\}]"""), "")
    return stripped.replace(Regex("\\s+"), " ").trim()
}

fun CopiedData.log(): String {
    return when (this) {
        is CopiedData.File -> filePaths.fastJoinToString()
        is CopiedData.FormattedText -> {
            "$mimeType: ${plainText.take(200)}"
        }

        is CopiedData.Image -> imagePath
        is CopiedData.Text -> "${text}, $dateTime, $id, $applicationId"
    }
}