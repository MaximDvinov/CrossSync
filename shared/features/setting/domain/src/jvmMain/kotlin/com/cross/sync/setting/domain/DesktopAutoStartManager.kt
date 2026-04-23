package com.cross.sync.setting.domain

import java.io.File

class DesktopAutoStartManager {
    private val osName = System.getProperty("os.name")?.lowercase().orEmpty()
    private val isMacOs = osName.contains("mac")

    private val launchAgentsDir: File by lazy {
        File(System.getProperty("user.home"), "Library/LaunchAgents")
    }

    private val plistFile: File by lazy {
        File(launchAgentsDir, "$AUTOSTART_LABEL.plist")
    }

    fun isSupported(): Boolean = isMacOs

    fun isEnabled(): Boolean {
        if (!isMacOs) return false
        return plistFile.exists()
    }

    fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
        if (!isMacOs) return@runCatching
        if (enabled) {
            enableAutoStart()
        } else {
            disableAutoStart()
        }
    }

    private fun enableAutoStart() {
        if (!launchAgentsDir.exists()) {
            launchAgentsDir.mkdirs()
        }
        plistFile.writeText(buildPlistContent())
        reloadLaunchAgent()
    }

    private fun disableAutoStart() {
        unloadLaunchAgent()
        if (plistFile.exists()) {
            plistFile.delete()
        }
    }

    private fun reloadLaunchAgent() {
        unloadLaunchAgent()
        runCommand("launchctl", "bootstrap", "gui/${resolveUid()}", plistFile.absolutePath)
    }

    private fun unloadLaunchAgent() {
        runCommand("launchctl", "bootout", "gui/${resolveUid()}", plistFile.absolutePath)
    }

    private fun resolveUid(): String {
        val process = ProcessBuilder("id", "-u")
            .redirectErrorStream(true)
            .start()
        return process.inputStream.bufferedReader().readText().trim().ifBlank { "501" }
    }

    private fun runCommand(vararg command: String) {
        runCatching {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            process.waitFor()
        }
    }

    private fun buildPlistContent(): String {
        return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>Label</key>
            |    <string>$AUTOSTART_LABEL</string>
            |    <key>ProgramArguments</key>
            |    <array>
            |        <string>/usr/bin/open</string>
            |        <string>-b</string>
            |        <string>$BUNDLE_ID</string>
            |    </array>
            |    <key>RunAtLoad</key>
            |    <true/>
            |    <key>KeepAlive</key>
            |    <false/>
            |</dict>
            |</plist>
        """.trimMargin()
    }

    companion object {
        private const val AUTOSTART_LABEL = "com.cross.sync.desktop.autostart"
        private const val BUNDLE_ID = "com.cross.sync.desktopApp"
    }
}
