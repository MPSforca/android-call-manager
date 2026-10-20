package com.sforca.callmanager.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TypeTest {
    @Test
    fun `scale uses one bundled family rather than the platform default`() {
        val family = CallManagerTypography.bodyLarge.fontFamily
        assertNotEquals(FontFamily.Default, family)
        assertSame(family, CallManagerTypography.titleMedium.fontFamily)
        assertSame(family, CallManagerTypography.labelSmall.fontFamily)
    }
}
