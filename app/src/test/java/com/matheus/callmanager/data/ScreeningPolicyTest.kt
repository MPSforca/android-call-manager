package com.matheus.callmanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the three required behaviors, independent of the real Android call APIs:
 * 1. Allowed calls ring normally.
 * 2. Calls that fail every rule are silenced by default (visible, but never ring).
 * 2.1. The user can switch that to an outright, invisible reject instead.
 */
class ScreeningPolicyTest {

    @Test
    fun `allowed calls ring normally regardless of blocked-call setting`() {
        val decision = ScreeningDecision.Allow("Known contact")

        assertEquals(
            ScreeningAction.ALLOW,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.SILENCE)
        )
        assertEquals(
            ScreeningAction.ALLOW,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.REJECT)
        )
    }

    @Test
    fun `blocked calls are silenced by default so they stay visible but do not ring`() {
        val decision = ScreeningDecision.Reject("On your block list")

        assertEquals(
            ScreeningAction.SILENCE,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.SILENCE)
        )
    }

    @Test
    fun `user can opt into an outright reject instead of silencing`() {
        val decision = ScreeningDecision.Reject("On your block list")

        assertEquals(
            ScreeningAction.REJECT,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.REJECT)
        )
    }
}
