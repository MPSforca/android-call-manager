package com.sforca.callmanager.feature.home

import android.provider.CallLog
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HomeSummaryTest {
    private val today = LocalDate.of(2026, 10, 18)
    private val zone = ZoneOffset.UTC

    private fun at(
        date: LocalDate,
        hour: Int = 12,
    ) = date.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun call(
        number: String,
        timestamp: Long,
        type: Int = CallLog.Calls.INCOMING_TYPE,
    ) = RecentCall(number, null, timestamp, type)

    private fun blocked(
        number: String,
        timestamp: Long,
    ) = UnifiedBlockedCall(number, null, timestamp, "rule", "Call Manager")

    @Test
    fun `counts only blocked calls from today`() {
        val summary =
            summarize(
                calls = emptyList(),
                blockedCalls =
                    listOf(
                        blocked("+1", at(today)),
                        blocked("+2", at(today, 8)),
                        blocked("+3", at(today.minusDays(1))),
                    ),
                today = today,
                zone = zone,
            )

        assertEquals(2, summary.blockedToday)
    }

    @Test
    fun `allowed today counts incoming and missed calls we did not block`() {
        val summary =
            summarize(
                calls =
                    listOf(
                        call("+1", at(today), CallLog.Calls.INCOMING_TYPE),
                        call("+2", at(today, 9), CallLog.Calls.MISSED_TYPE),
                        call("+3", at(today, 10), CallLog.Calls.OUTGOING_TYPE),
                        call("+4", at(today, 11), CallLog.Calls.BLOCKED_TYPE),
                        call("+5", at(today.minusDays(1)), CallLog.Calls.INCOMING_TYPE),
                    ),
                blockedCalls = emptyList(),
                today = today,
                zone = zone,
            )

        assertEquals(2, summary.allowedToday)
    }

    @Test
    fun `a call our own log blocked is not counted as allowed`() {
        val stamp = at(today)
        val summary =
            summarize(
                calls = listOf(call("+15550000001", stamp, CallLog.Calls.MISSED_TYPE)),
                blockedCalls = listOf(blocked("+15550000001", stamp)),
                today = today,
                zone = zone,
            )

        assertEquals(0, summary.allowedToday)
    }

    @Test
    fun `the activity feed groups by number and keeps the newest few`() {
        val calls = (1..8).map { call("+155500000$it", at(today, it)) }
        val summary = summarize(calls, emptyList(), today, zone, feedSize = 3)

        assertEquals(3, summary.activity.size)
    }

    @Test
    fun `a number with several calls is one feed row`() {
        val calls = listOf(call("+15550000001", at(today, 9)), call("+15550000001", at(today, 8)))

        val summary = summarize(calls, emptyList(), today, zone)

        assertEquals(1, summary.activity.size)
        assertEquals(
            2,
            summary.activity
                .single()
                .calls.size,
        )
    }
}
