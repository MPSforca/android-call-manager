package com.sforca.callmanager.core.model

/** Which light/dark scheme the app uses. */
enum class ThemeMode {
    /** Follow the device's day/night setting. */
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Resolves to a concrete dark/light choice; [systemDark] is only consulted for [SYSTEM]. */
    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
}
