package utils

import javax.swing.SwingUtilities

fun bringAppWindowsToFront() {
    SwingUtilities.invokeLater {
        java.awt.Window.getWindows().forEach { w ->
            try {
                if (w.isVisible) {
                    w.toFront()
                    w.requestFocus()
                }
            } catch (_: Throwable) { /* ignore individual failures */
            }
        }
    }
}

fun bringAppToFront(application: String) {
    try {
        Runtime.getRuntime().exec(
            arrayOf(
                "osascript",
                "-e",
                "tell application id \"$application\" to activate"
            )
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getFrontmostAppBundleId(): String? {
    val process = Runtime.getRuntime().exec(arrayOf(
        "osascript",
        "-e",
        "id of application (path to frontmost application as text)"
    ))
    return process.inputStream.bufferedReader().readText().trim().ifEmpty { null }
}