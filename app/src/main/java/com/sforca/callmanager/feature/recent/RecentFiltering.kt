package com.sforca.callmanager.feature.recent

import android.provider.CallLog
import com.sforca.callmanager.core.data.BlockedCallMerger
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.RecentCallGroup
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The filter chips above the Recent feed. */
enum class RecentFilter { ALL, BLOCKED, MISSED, ANSWERED }

/** One local calendar [date] of the feed, with that day's calls grouped by number. */
data class DayGroup(
    val date: LocalDate,
    val groups: List<RecentCallGroup>,
)

/**
 * Applies a [filter] chip and a free-text [query] (contact name or any digits of the number).
 * "Blocked" is decided by [BlockedCallMerger.wasBlocked] as well as the system call type, because
 * a call this app silenced never gets the system's blocked tag (see `callOutcomeLabel`).
 */
internal fun List<RecentCall>.filterCalls(
    filter: RecentFilter,
    query: String,
    blockedCalls: List<UnifiedBlockedCall>,
): List<RecentCall> {
    val trimmed = query.trim()
    val queryDigits = trimmed.filter { it.isDigit() }
    return filter { call ->
        val blocked = call.isBlocked(blockedCalls)
        val matchesFilter =
            when (filter) {
                RecentFilter.ALL -> true
                RecentFilter.BLOCKED -> blocked
                RecentFilter.MISSED -> !blocked && call.type == CallLog.Calls.MISSED_TYPE
                RecentFilter.ANSWERED -> !blocked && call.type == CallLog.Calls.INCOMING_TYPE
            }
        matchesFilter &&
            (
                trimmed.isEmpty() ||
                    call.name?.contains(trimmed, ignoreCase = true) == true ||
                    (queryDigits.isNotEmpty() && call.number.filter { it.isDigit() }.contains(queryDigits))
            )
    }
}

internal fun RecentCall.isBlocked(blockedCalls: List<UnifiedBlockedCall>): Boolean =
    type == CallLog.Calls.BLOCKED_TYPE ||
        type == CallLog.Calls.REJECTED_TYPE ||
        BlockedCallMerger.wasBlocked(number, timestamp, blockedCalls)

/**
 * Buckets calls by local date in [zone], then groups each day's calls by number. Days, the numbers
 * within a day and the calls within a number are all newest first.
 */
internal fun List<RecentCall>.groupByDay(zone: ZoneId): List<DayGroup> =
    groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .map { (date, calls) -> DayGroup(date, CallLogUtil.groupByNumber(calls)) }
        .sortedByDescending { it.date }
