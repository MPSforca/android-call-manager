package com.sforca.callmanager.core.telephony

import android.provider.CallLog
import com.sforca.callmanager.R
import com.sforca.callmanager.core.model.RecentCall
import org.junit.Assert.assertEquals
import org.junit.Test

class CallLogUtilTest {
    @Test
    fun `call type label describes the outcome of a call`() {
        assertEquals(R.string.call_outcome_incoming, CallLogUtil.callTypeLabel(CallLog.Calls.INCOMING_TYPE))
        assertEquals(R.string.call_outcome_outgoing, CallLogUtil.callTypeLabel(CallLog.Calls.OUTGOING_TYPE))
        assertEquals(R.string.call_outcome_missed, CallLogUtil.callTypeLabel(CallLog.Calls.MISSED_TYPE))
        assertEquals(R.string.call_outcome_voicemail, CallLogUtil.callTypeLabel(CallLog.Calls.VOICEMAIL_TYPE))
        assertEquals(R.string.call_outcome_rejected, CallLogUtil.callTypeLabel(CallLog.Calls.REJECTED_TYPE))
        assertEquals(R.string.call_outcome_blocked, CallLogUtil.callTypeLabel(CallLog.Calls.BLOCKED_TYPE))
        assertEquals(
            R.string.call_outcome_answered_elsewhere,
            CallLogUtil.callTypeLabel(CallLog.Calls.ANSWERED_EXTERNALLY_TYPE),
        )
        assertEquals(R.string.call_outcome_call, CallLogUtil.callTypeLabel(-1))
    }

    @Test
    fun `calls from the same number are grouped into one entry`() {
        val calls =
            listOf(
                RecentCall(number = "+15551111111", name = "Spammer", timestamp = 2_000, type = 1),
                RecentCall(number = "+15551111111", name = "Spammer", timestamp = 1_000, type = 1),
            )

        val groups = CallLogUtil.groupByNumber(calls)

        assertEquals(1, groups.size)
        assertEquals(2, groups.first().calls.size)
    }

    @Test
    fun `group calls are ordered newest first regardless of input order`() {
        val calls =
            listOf(
                RecentCall(number = "+15551111111", name = null, timestamp = 1_000, type = 1),
                RecentCall(number = "+15551111111", name = null, timestamp = 3_000, type = 1),
                RecentCall(number = "+15551111111", name = null, timestamp = 2_000, type = 1),
            )

        val group = CallLogUtil.groupByNumber(calls).first()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), group.calls.map { it.timestamp })
        assertEquals(3_000L, group.latestTimestamp)
    }

    @Test
    fun `groups are ordered by their most recent call`() {
        val calls =
            listOf(
                RecentCall(number = "+15551111111", name = null, timestamp = 1_000, type = 1),
                RecentCall(number = "+15552222222", name = null, timestamp = 5_000, type = 1),
            )

        val groups = CallLogUtil.groupByNumber(calls)

        assertEquals("+15552222222", groups.first().number)
        assertEquals("+15551111111", groups.last().number)
    }

    @Test
    fun `a name from any call in the group is used even if the latest call has none`() {
        val calls =
            listOf(
                RecentCall(number = "+15551111111", name = null, timestamp = 2_000, type = 1),
                RecentCall(number = "+15551111111", name = "Spammer", timestamp = 1_000, type = 1),
            )

        val group = CallLogUtil.groupByNumber(calls).first()

        assertEquals("Spammer", group.name)
    }
}
