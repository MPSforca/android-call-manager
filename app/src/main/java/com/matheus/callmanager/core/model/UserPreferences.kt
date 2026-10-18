package com.matheus.callmanager.core.model

/** The user's screening rule settings. Defaults here are the single source of truth for fresh installs. */
data class UserPreferences(
    val allowContacts: Boolean = true,
    val recentAcceptedRuleEnabled: Boolean = true,
    /** Days after answering a caller that they stay allowed; the rule is toggled by [recentAcceptedRuleEnabled]. */
    val allowRecentAcceptedDays: Int = 30,
    /** Allow callers the user has called back/out to within [allowRecentOutgoingDays]. */
    val recentOutgoingRuleEnabled: Boolean = true,
    val allowRecentOutgoingDays: Int = 30,
    /** When true, any number not covered by an allow rule and not on the block list is rejected too. */
    val blockUnknownNumbers: Boolean = false,
    /** How calls that fail every allow rule (block list, unknown numbers) are handled. Defaults to silent. */
    val blockedCallAction: BlockedCallAction = BlockedCallAction.SILENCE,
    /** Whether an outright-rejected call posts a notification bar heads-up. Defaults to on. */
    val showRejectedCallNotification: Boolean = true,
)
