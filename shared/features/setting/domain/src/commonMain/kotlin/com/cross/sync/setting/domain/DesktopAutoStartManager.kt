package com.cross.sync.setting.domain

interface DesktopAutoStartManager {
    fun isSupported(): Boolean
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean): Result<Unit>
}
