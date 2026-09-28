package com.matheus.callmanager.core.model

/** What Telecom should actually do with the call, independent of the Android call-screening API shape. */
enum class ScreeningAction {
    /** Ring/vibrate as normal. */
    ALLOW,

    /** Still allowed through (answerable) but muted: no ringtone/vibration. */
    SILENCE,

    /** Rejected outright; never surfaces as an incoming call. */
    REJECT,
}
