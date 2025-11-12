package utils

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.cross.sync.clipboard.domain.entity.Application
import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import toBufferedImage
import java.io.File
import javax.imageio.ImageIO

fun pasteClipboardMac() {
    Runtime.getRuntime().exec(
        arrayOf(
            "osascript",
            "-e",
            "tell application \"System Events\" to keystroke \"v\" using command down"
        )
    )
}

fun getFrontmostAppBundleId(): String? {
    val process = Runtime.getRuntime().exec(
        arrayOf(
            "osascript",
            "-e",
            "id of application (path to frontmost application as text)"
        )
    )
    return process.inputStream.bufferedReader().readText().trim().ifEmpty { null }
}

fun getInstalledApplications(): List<Application> {
    val appDirs = listOf(
        File("/Applications"),
        File(System.getProperty("user.home"), "Applications"),
        File("/System/Applications"),
        File("/Applications/Utilities")
    )

    val seenBundleIds = mutableSetOf<String>()

    return appDirs.flatMap { dir ->
        try {
            if (!dir.exists() || !dir.isDirectory) return@flatMap emptyList()

            val topLevelApps =
                dir.listFiles()?.filter { it.isDirectory && it.extension == "app" } ?: emptyList()

            topLevelApps.mapNotNull { appDir ->
                try {
                    val app = parseAppBundle(appDir)
                    val uniqueKey = app?.let { it.id.ifEmpty { it.name + "@" + it.path } }
                        ?: return@mapNotNull null
                    if (uniqueKey in seenBundleIds) return@mapNotNull null
                    seenBundleIds += uniqueKey
                    app
                } catch (e: Exception) {
                    System.err.println("Failed parse app ${appDir.absolutePath}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed scan directory ${dir.absolutePath}: ${e.message}")
            emptyList()
        }
    }
}

fun parseAppBundle(appDir: File): Application? {
    val plistFile = File(appDir, "Contents/Info.plist")
    if (!plistFile.exists()) return null

    val dict = PropertyListParser.parse(plistFile) as? NSDictionary ?: return null

    val name = dict.objectForKey("CFBundleDisplayName")?.toString()
        ?: dict.objectForKey("CFBundleName")?.toString()
        ?: appDir.nameWithoutExtension

    val bundleId = dict.objectForKey("CFBundleIdentifier")?.toString() ?: ""

    val iconName = dict.objectForKey("CFBundleIconFile")?.toString()

    val iconPath = iconName?.let {
        val byName = File(appDir, "Contents/Resources/$it")
        when {
            byName.exists() -> byName.absolutePath
            File(appDir, "Contents/Resources/$it.icns").exists() -> File(
                appDir,
                "Contents/Resources/$it.icns"
            ).absolutePath

            else -> null
        }
    } ?: run {
        val resources = File(appDir, "Contents/Resources")
        resources.listFiles()?.firstOrNull { f -> f.isFile && f.extension == "icns" }?.absolutePath
    }

    val newIconPath = iconPath?.let { saveAppIconInLocal(it, name) }

    return Application(id = bundleId, name = name, icon = newIconPath, path = appDir.absolutePath)
}

fun saveAppIconInLocal(iconPath: String, name: String): String? {
    val file = File(System.getProperty("user.home"), ".crosssync/appImages")

    return try {
        val imageFile = File(iconPath)
        if (imageFile.exists()) {
            val image = ImageIO.read(convertIcnsToPng(imageFile))
            if (!file.exists()) file.mkdirs()
            val bufferedImage = toBufferedImage(image)
            ImageIO.write(bufferedImage, "png", File(file, "/${name.trim()}.png"))
            bufferedImage.flush()
            "${file.absolutePath}/${name.trim()}.png"
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun convertIcnsToPng(icnsFile: File): File? {
    val output = File.createTempFile("icon_", ".png")
    val process = ProcessBuilder(
        "sips", "-s", "format", "png", icnsFile.absolutePath, "--out", output.absolutePath
    ).redirectErrorStream(true).start()

    val exitCode = process.waitFor()
    return if (exitCode == 0 && output.exists()) output else null
}