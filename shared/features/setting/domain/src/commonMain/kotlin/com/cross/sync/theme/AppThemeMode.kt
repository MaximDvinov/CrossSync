package com.cross.sync.theme

enum class AppThemeMode(
    val label: String,
) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun next(): AppThemeMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStoredValue(value: String): AppThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

