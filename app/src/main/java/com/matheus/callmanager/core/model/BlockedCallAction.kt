package com.matheus.callmanager.core.model

/** How a call that fails every allow rule is handled. */
enum class BlockedCallAction {
    /** Let it through silently: it still shows on screen/notification, but never rings or vibrates. */
    SILENCE,

    /** Reject it outright, like the user declined it; it never surfaces as an incoming call at all. */
    REJECT,
}
