package com.matheus.callmanager.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure matching logic for allow-listing callers by their carrier-supplied caller ID name (CNAM),
 * e.g. "PagerDuty" regardless of which of PagerDuty's rotating numbers actually called.
 */
class NameAllowMatcherTest {
    @Test
    fun `caller name containing a pattern matches`() {
        assertTrue(NameAllowMatcher.matches("PagerDuty Outgoing Numbers", listOf("PagerDuty")))
    }

    @Test
    fun `match is case-insensitive`() {
        assertTrue(NameAllowMatcher.matches("pagerduty outgoing numbers", listOf("PagerDuty")))
    }

    @Test
    fun `caller name not containing any pattern does not match`() {
        assertFalse(NameAllowMatcher.matches("Some Spammer", listOf("PagerDuty")))
    }

    @Test
    fun `null caller name never matches`() {
        assertFalse(NameAllowMatcher.matches(null, listOf("PagerDuty")))
    }

    @Test
    fun `blank caller name never matches`() {
        assertFalse(NameAllowMatcher.matches("", listOf("PagerDuty")))
    }

    @Test
    fun `blank patterns are ignored`() {
        assertFalse(NameAllowMatcher.matches("Anyone", listOf("", "  ")))
    }

    @Test
    fun `no patterns configured means no match`() {
        assertFalse(NameAllowMatcher.matches("PagerDuty Outgoing Numbers", emptyList()))
    }
}
