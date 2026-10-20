package com.sforca.callmanager.feature.home

import android.provider.CallLog
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.RecentCallGroup
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogUtil
import com.sforca.callmanager.feature.recent.isBlocked
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val DEFAULT_FEED_SIZE = 5

/** What the Home tab shows below the protection header. */
data class HomeSummary(
    val blockedToday: Int,
    val allowedToday: Int,
    val activity: List<RecentCallGroup>,
)

/**
 * Counts today's blocked calls and the incoming or missed calls we let through, and picks the
 * newest [feedSize] numbers (calls grouped by number, as on the Recent tab) for the activity feed.
 * [calls] is expected newest first.
 */
internal fun summarize(
    calls: List<RecentCall>,
    blockedCalls: List<UnifiedBlockedCall>,
    today: LocalDate,
    zone: ZoneId,
    feedSize: Int = DEFAULT_FEED_SIZE,
): HomeSummary {
    fun isToday(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate() == today
    val allowed =
        calls.count {
            isToday(it.timestamp) &&
                (it.type == CallLog.Calls.INCOMING_TYPE || it.type == CallLog.Calls.MISSED_TYPE) &&
                !it.isBlocked(blockedCalls)
        }
    return HomeSummary(
        blockedToday = blockedCalls.count { isToday(it.timestamp) },
        allowedToday = allowed,
        activity = CallLogUtil.groupByNumber(calls).take(feedSize),
    )
}
