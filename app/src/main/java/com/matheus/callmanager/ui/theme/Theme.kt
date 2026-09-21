package com.matheus.callmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

/**
 * On API 31+ (Material You), themes the app from the user's wallpaper-derived system palette
 * instead of the static Material 3 baseline colors. Older devices keep the static palette — dynamic
 * color isn't available below API 31.
 */
@Composable
fun CallManagerTheme(content: @Composable () -> Unit) {
    val useDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        useDynamicColor && isDark -> dynamicDarkColorScheme(context)
        useDynamicColor -> dynamicLightColorScheme(context)
        isDark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
