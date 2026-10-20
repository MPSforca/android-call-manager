package com.sforca.callmanager.core.data

import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.ScreeningAction
import com.sforca.callmanager.core.model.ScreeningDecision

/**
 * Pure mapping from a rule-evaluation result to what should happen to the call. Kept free of any
 * android.telecom types so it can be unit tested without Robolectric or a device.
 */
object ScreeningPolicy {
    fun determineAction(
        decision: ScreeningDecision,
        blockedCallAction: BlockedCallAction,
    ): ScreeningAction =
        when (decision) {
            is ScreeningDecision.Allow -> ScreeningAction.ALLOW
            is ScreeningDecision.Reject ->
                when (blockedCallAction) {
                    BlockedCallAction.SILENCE -> ScreeningAction.SILENCE
                    BlockedCallAction.REJECT -> ScreeningAction.REJECT
                }
        }

    /**
     * Whether the call vanished from the incoming-call UI entirely and so is worth surfacing via a
     * notification instead. True for an outright [ScreeningAction.REJECT], and also for
     * [ScreeningAction.SILENCE] below API 29, where there's no silence primitive and it falls back
     * to a full reject too (see RuleBasedCallScreeningService.buildResponse).
     */
    fun shouldNotifyRejected(
        action: ScreeningAction,
        sdkInt: Int,
    ): Boolean =
        when (action) {
            ScreeningAction.REJECT -> true
            ScreeningAction.SILENCE -> sdkInt < 29
            ScreeningAction.ALLOW -> false
        }
}
