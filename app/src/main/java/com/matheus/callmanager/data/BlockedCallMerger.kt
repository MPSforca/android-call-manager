package com.matheus.callmanager.data

import com.matheus.callmanager.util.SystemBlockedCall
import kotlin.math.abs

/** A blocked call for display, whichever source flagged it. */
data class UnifiedBlockedCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    val reason: String,
    val source: String
)

/**
 * Merges this app's own block log with every call the system call log tagged BLOCKED_TYPE —
 * covering Android's native "Blocked numbers" list, the carrier, and other call-screening apps —
 * so the Blocked tab isn't limited to calls this app happened to be running for.
 *
 * Our own rejected calls land in both [ownLog] (Room, with a specific rule reason) and [systemLog]
 * (the system call log, generically attributed to "a call-screening app"). Prefer the richer
 * app-side entry when the two clearly describe the same call — same number within 5 seconds.
 */
object BlockedCallMerger {
    private const val DEDUPE_WINDOW_MILLIS = 5_000L

    /** Digits used to match numbers across log sources that may format the same call differently
     * (e.g. "+15551234567" vs "5551234567"). Comparing only the last few digits, rather than the
     * full string, tolerates a missing/differing country code or leading '+'/'1'. */
    private const val MATCH_SUFFIX_DIGITS = 10

    private fun matchKey(number: String): String {
        val digits = number.filter { it.isDigit() }
        return digits.takeLast(MATCH_SUFFIX_DIGITS)
    }

    private fun sameNumber(a: String, b: String): Boolean {
        val keyA = matchKey(a)
        val keyB = matchKey(b)
        return keyA.isNotEmpty() && keyA == keyB
    }

    fun merge(ownLog: List<BlockedCallLog>, systemLog: List<SystemBlockedCall>): List<UnifiedBlockedCall> {
        val ownEntries = ownLog.map {
            UnifiedBlockedCall(it.number, it.callerName, it.timestamp, it.reason, "Call Manager")
        }
        val systemOnly = systemLog
            .filterNot { sys -> ownLog.any { sameNumber(it.number, sys.number) && abs(it.timestamp - sys.timestamp) < DEDUPE_WINDOW_MILLIS } }
            .map { UnifiedBlockedCall(it.number, it.name, it.timestamp, it.source, "Android") }
        return (ownEntries + systemOnly).sortedByDescending { it.timestamp }
    }
}
