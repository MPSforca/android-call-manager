package com.sforca.callmanager.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarInitialsTest {
    @Test
    fun `two-word name takes first letter of each word`() {
        assertEquals("AL", initialsOf("Ada Lovelace"))
    }

    @Test
    fun `single-word name takes its first letter`() {
        assertEquals("G", initialsOf("Grace"))
    }

    @Test
    fun `null name falls back to a placeholder`() {
        assertEquals("?", initialsOf(null))
    }

    @Test
    fun `blank name falls back to a placeholder`() {
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `extra whitespace between words is ignored`() {
        assertEquals("AL", initialsOf("Ada   Lovelace"))
    }

    @Test
    fun `more than two words uses only the first and last`() {
        assertEquals("AL", initialsOf("Ada Marie Lovelace"))
    }
}
