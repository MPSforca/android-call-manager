package com.matheus.callmanager.core.model

data class SystemBlockedCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    /** Human-readable source, derived from CallLog.Calls.BLOCK_REASON where available (API 29+). */
    val source: String,
)
