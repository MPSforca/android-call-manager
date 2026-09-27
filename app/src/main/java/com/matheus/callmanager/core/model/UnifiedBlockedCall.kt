package com.matheus.callmanager.core.model

/** A blocked call for display, whichever source flagged it. */
data class UnifiedBlockedCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    val reason: String,
    val source: String,
)
