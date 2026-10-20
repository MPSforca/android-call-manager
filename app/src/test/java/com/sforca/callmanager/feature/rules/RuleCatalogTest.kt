package com.sforca.callmanager.feature.rules

import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleCatalogTest {
    @Test
    fun `catalog describes every rule exactly once, in chain order`() {
        assertEquals(ScreeningRuleId.entries, RuleCatalog.descriptors.map { it.id })
        assertEquals((1..ScreeningRuleId.entries.size).toList(), RuleCatalog.descriptors.map { it.order })
    }

    @Test
    fun `every descriptor has distinct wording`() {
        val strings = RuleCatalog.descriptors.flatMap { listOf(it.title, it.description, it.example) }
        assertEquals(strings.size, strings.toSet().size)
    }

    @Test
    fun `rules are grouped by step in step order`() {
        assertEquals(
            RulesStep.entries.toSet(),
            RuleCatalog.descriptors.map { it.step }.toSet() + RulesStep.BLOCKED_CALLS,
        )
        assertEquals(
            RuleCatalog.descriptors.map { it.step },
            RuleCatalog.descriptors.map { it.step }.sortedBy { it.ordinal },
        )
        assertEquals(
            listOf(ScreeningRuleId.CONTACT, ScreeningRuleId.RECENTLY_ACCEPTED, ScreeningRuleId.RECENTLY_CALLED),
            RuleCatalog.forStep(RulesStep.PEOPLE_YOU_KNOW).map { it.id },
        )
        assertTrue(RuleCatalog.forStep(RulesStep.BLOCKED_CALLS).isEmpty())
    }

    @Test
    fun `forRule finds a descriptor`() {
        assertEquals(RulesStep.UNKNOWN_CALLERS, RuleCatalog.forRule(ScreeningRuleId.UNKNOWN_FALLBACK).step)
    }

    @Test
    fun `lists summary carries the counts`() {
        val counts = ListCounts(blocked = 2, allowed = 1, namePatterns = 3)
        assertEquals(StepSummary.Lists(counts), summarize(RulesStep.LISTS, UserPreferences(), counts))
        assertEquals(StepSummary.Lists(ListCounts()), summarize(RulesStep.LISTS, UserPreferences()))
    }

    @Test
    fun `people summary counts enabled toggles`() {
        assertEquals(StepSummary.Toggles(3, 3), summarize(RulesStep.PEOPLE_YOU_KNOW, UserPreferences()))
        val prefs = UserPreferences(allowContacts = false, recentOutgoingRuleEnabled = false)
        assertEquals(StepSummary.Toggles(1, 3), summarize(RulesStep.PEOPLE_YOU_KNOW, prefs))
    }

    @Test
    fun `unknown summary reflects the toggle`() {
        assertEquals(StepSummary.UnknownCallers(false), summarize(RulesStep.UNKNOWN_CALLERS, UserPreferences()))
        val prefs = UserPreferences(blockUnknownNumbers = true)
        assertEquals(StepSummary.UnknownCallers(true), summarize(RulesStep.UNKNOWN_CALLERS, prefs))
    }

    @Test
    fun `blocked summary only notifies when rejecting`() {
        val silence =
            UserPreferences(blockedCallAction = BlockedCallAction.SILENCE, showRejectedCallNotification = true)
        assertFalse((summarize(RulesStep.BLOCKED_CALLS, silence) as StepSummary.BlockedCalls).notifies)

        val reject = silence.copy(blockedCallAction = BlockedCallAction.REJECT)
        assertEquals(
            StepSummary.BlockedCalls(BlockedCallAction.REJECT, true),
            summarize(RulesStep.BLOCKED_CALLS, reject),
        )

        val quiet = reject.copy(showRejectedCallNotification = false)
        assertFalse((summarize(RulesStep.BLOCKED_CALLS, quiet) as StepSummary.BlockedCalls).notifies)
    }
}
