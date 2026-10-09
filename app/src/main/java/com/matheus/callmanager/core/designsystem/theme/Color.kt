package com.matheus.callmanager.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand seed: a cool indigo/teal, chosen for a "security/utility" feel rather than stock Material
// purple. These are the only two colors handed to us by brand; every other role below is derived
// from them so light/dark stay in sync.
private val BrandPrimary = Color(0xFF2E5AAC)
private val BrandSecondary = Color(0xFF3E6F6B)
private val BrandTertiary = Color(0xFF7A5295)

val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = BrandPrimary,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E2FF),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = BrandSecondary,
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFBFF0E9),
        onSecondaryContainer = Color(0xFF00201D),
        tertiary = BrandTertiary,
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF4D9FF),
        onTertiaryContainer = Color(0xFF30134A),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAF8FF),
        onBackground = Color(0xFF1A1B20),
        surface = Color(0xFFFAF8FF),
        onSurface = Color(0xFF1A1B20),
        surfaceVariant = Color(0xFFE1E2EC),
        onSurfaceVariant = Color(0xFF44474E),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
    )

val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFADC6FF),
        onPrimary = Color(0xFF002E69),
        primaryContainer = Color(0xFF194094),
        onPrimaryContainer = Color(0xFFD6E2FF),
        secondary = Color(0xFFA3D3CC),
        onSecondary = Color(0xFF053732),
        secondaryContainer = Color(0xFF254E49),
        onSecondaryContainer = Color(0xFFBFF0E9),
        tertiary = Color(0xFFE2B8FC),
        onTertiary = Color(0xFF48235F),
        tertiaryContainer = Color(0xFF603A77),
        onTertiaryContainer = Color(0xFFF4D9FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE3E2E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE3E2E9),
        surfaceVariant = Color(0xFF44474E),
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
    )

/**
 * Semantic colors Material doesn't define that this app needs at a glance: the outcome of a call.
 * Kept separate from [ColorScheme] roles since they map to domain concepts (blocked/allowed/
 * silenced/unknown), not Material's generic primary/secondary/tertiary slots.
 */
@Immutable
data class CallManagerColors(
    val blocked: Color,
    val onBlocked: Color,
    val blockedContainer: Color,
    val onBlockedContainer: Color,
    val allowed: Color,
    val onAllowed: Color,
    val allowedContainer: Color,
    val onAllowedContainer: Color,
    val silenced: Color,
    val onSilenced: Color,
    val silencedContainer: Color,
    val onSilencedContainer: Color,
    val unknown: Color,
    val onUnknown: Color,
    val unknownContainer: Color,
    val onUnknownContainer: Color,
)

val LightCallManagerColors =
    CallManagerColors(
        blocked = Color(0xFFBA1A1A),
        onBlocked = Color(0xFFFFFFFF),
        blockedContainer = Color(0xFFFFDAD6),
        onBlockedContainer = Color(0xFF410002),
        allowed = Color(0xFF2E6E3E),
        onAllowed = Color(0xFFFFFFFF),
        allowedContainer = Color(0xFFB4F2BE),
        onAllowedContainer = Color(0xFF002109),
        silenced = Color(0xFF8A5500),
        onSilenced = Color(0xFFFFFFFF),
        silencedContainer = Color(0xFFFFDDB0),
        onSilencedContainer = Color(0xFF2B1700),
        unknown = Color(0xFF5B5F6A),
        onUnknown = Color(0xFFFFFFFF),
        unknownContainer = Color(0xFFDFE2EC),
        onUnknownContainer = Color(0xFF181C25),
    )

val DarkCallManagerColors =
    CallManagerColors(
        blocked = Color(0xFFFFB4AB),
        onBlocked = Color(0xFF690005),
        blockedContainer = Color(0xFF93000A),
        onBlockedContainer = Color(0xFFFFDAD6),
        allowed = Color(0xFF99D5A4),
        onAllowed = Color(0xFF00390E),
        allowedContainer = Color(0xFF15531F),
        onAllowedContainer = Color(0xFFB4F2BE),
        silenced = Color(0xFFFFB955),
        onSilenced = Color(0xFF462A00),
        silencedContainer = Color(0xFF693F00),
        onSilencedContainer = Color(0xFFFFDDB0),
        unknown = Color(0xFFC4C6D2),
        onUnknown = Color(0xFF2D3139),
        unknownContainer = Color(0xFF434750),
        onUnknownContainer = Color(0xFFDFE2EC),
    )

val LocalCallManagerColors =
    staticCompositionLocalOf {
        LightCallManagerColors
    }
