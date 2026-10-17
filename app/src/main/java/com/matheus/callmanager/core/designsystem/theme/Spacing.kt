package com.matheus.callmanager.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing/radius scale replacing the literal `16.dp`/`8.dp` values scattered through the current
 * screens. Named by size rather than usage, so it stays meaningful as new screens adopt it.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
