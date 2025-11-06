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