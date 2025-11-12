package utils

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
    val process = Runtime.getRuntime().exec(arrayOf(
        "osascript",
        "-e",
        "id of application (path to frontmost application as text)"
    ))
    return process.inputStream.bufferedReader().readText().trim().ifEmpty { null }
}