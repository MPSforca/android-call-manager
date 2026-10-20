package com.sforca.callmanager.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA (4.5:1) for every text/background pair the app draws, in both themes. */
class ContrastTest {
    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertAa(
        name: String,
        fg: Color,
        bg: Color,
    ) {
        val r = ratio(fg, bg)
        assertTrue("$name contrast ${"%.2f".format(r)} < 4.5", r >= 4.5)
    }

    private fun semanticPairs(c: CallManagerColors) =
        mapOf(
            "blocked" to (c.onBlocked to c.blocked),
            "blockedContainer" to (c.onBlockedContainer to c.blockedContainer),
            "allowed" to (c.onAllowed to c.allowed),
            "allowedContainer" to (c.onAllowedContainer to c.allowedContainer),
            "silenced" to (c.onSilenced to c.silenced),
            "silencedContainer" to (c.onSilencedContainer to c.silencedContainer),
            "unknown" to (c.onUnknown to c.unknown),
            "unknownContainer" to (c.onUnknownContainer to c.unknownContainer),
        )

    @Test
    fun `semantic call outcome colors meet AA in light and dark`() {
        mapOf("light" to LightCallManagerColors, "dark" to DarkCallManagerColors).forEach { (theme, colors) ->
            semanticPairs(colors).forEach { (name, pair) -> assertAa("$theme $name", pair.first, pair.second) }
        }
    }

    @Test
    fun `brand color scheme text pairs meet AA in light and dark`() {
        mapOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (theme, s) ->
            assertAa("$theme onSurface", s.onSurface, s.surface)
            assertAa("$theme onSurfaceVariant", s.onSurfaceVariant, s.surface)
            assertAa("$theme onPrimary", s.onPrimary, s.primary)
            assertAa("$theme onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer)
            assertAa("$theme onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
            assertAa("$theme onError", s.onError, s.error)
            assertAa("$theme onErrorContainer", s.onErrorContainer, s.errorContainer)
            assertAa("$theme primary on surface", s.primary, s.surface)
        }
    }
}
