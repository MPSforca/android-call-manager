package com.sforca.callmanager.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * App theme. Dynamic color (Material You, API 31+) is now an explicit opt-in via [dynamicColor]
 * rather than an unconditional override — it defaults to false so the branded [LightColorScheme]/
 * [DarkColorScheme] palette defined in Color.kt is actually what people see. A caller (the Rules
 * screen, via a user setting) can pass `dynamicColor = true` to let the wallpaper-derived system
 * palette take over on API 31+; below API 31 this parameter has no effect.
 */
@Composable
fun CallManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val canUseDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme =
        when {
            canUseDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            canUseDynamicColor -> dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    val callManagerColors = if (darkTheme) DarkCallManagerColors else LightCallManagerColors

    CompositionLocalProvider(
        LocalCallManagerColors provides callManagerColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CallManagerTypography,
            shapes = CallManagerShapes,
            content = content,
        )
    }
}
