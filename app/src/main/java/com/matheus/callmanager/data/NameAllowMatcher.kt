package com.matheus.callmanager.data

/**
 * Pure matching logic for allow-listing callers by their carrier-supplied caller ID name (CNAM)
 * instead of by number — needed for callers like PagerDuty whose caller ID name stays consistent
 * even though the number that actually calls rotates. Kept free of Android types so it's cheap to
 * unit test.
 */
object NameAllowMatcher {
    fun matches(callerName: String?, patterns: List<String>): Boolean {
        if (callerName.isNullOrBlank()) return false
        return patterns.any { it.isNotBlank() && callerName.contains(it, ignoreCase = true) }
    }
}
