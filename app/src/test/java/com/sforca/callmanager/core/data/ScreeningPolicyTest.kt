package com.sforca.callmanager.core.data

import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.ScreeningAction
import com.sforca.callmanager.core.model.ScreeningDecision
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
            ScreeningPolicy.determineAction(decision, BlockedCallAction.SILENCE),
        )
        assertEquals(
            ScreeningAction.ALLOW,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.REJECT),
        )
    }

    @Test
    fun `blocked calls are silenced by default so they stay visible but do not ring`() {
        val decision = ScreeningDecision.Reject("On your block list")

        assertEquals(
            ScreeningAction.SILENCE,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.SILENCE),
        )
    }

    @Test
    fun `user can opt into an outright reject instead of silencing`() {
        val decision = ScreeningDecision.Reject("On your block list")

        assertEquals(
            ScreeningAction.REJECT,
            ScreeningPolicy.determineAction(decision, BlockedCallAction.REJECT),
        )
    }

    @Test
    fun `an outright reject always warrants a notification`() {
        assertEquals(true, ScreeningPolicy.shouldNotifyRejected(ScreeningAction.REJECT, sdkInt = 34))
        assertEquals(true, ScreeningPolicy.shouldNotifyRejected(ScreeningAction.REJECT, sdkInt = 28))
    }

    @Test
    fun `a silenced call warrants a notification only when it actually fell back to a full reject`() {
        // Below API 29 there's no silence primitive, so SILENCE falls back to a full reject (see
        // RuleBasedCallScreeningService.buildResponse) and should notify like any other reject.
        assertEquals(false, ScreeningPolicy.shouldNotifyRejected(ScreeningAction.SILENCE, sdkInt = 29))
        assertEquals(true, ScreeningPolicy.shouldNotifyRejected(ScreeningAction.SILENCE, sdkInt = 28))
    }

    @Test
    fun `an allowed call never warrants a notification`() {
        assertEquals(false, ScreeningPolicy.shouldNotifyRejected(ScreeningAction.ALLOW, sdkInt = 34))
    }
}
