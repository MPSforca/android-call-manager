package com.matheus.callmanager.data

/** What Telecom should actually do with the call, independent of the Android call-screening API shape. */
enum class ScreeningAction {
    /** Ring/vibrate as normal. */
    ALLOW,

    /** Still allowed through (answerable) but muted: no ringtone/vibration. */
    SILENCE,

    /** Rejected outright; never surfaces as an incoming call. */
    REJECT,
}

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
