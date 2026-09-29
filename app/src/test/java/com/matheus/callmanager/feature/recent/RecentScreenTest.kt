package com.matheus.callmanager.feature.recent

import android.provider.CallLog
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentScreenTest {
    @Test
    fun `statusOf reports BLOCKED when the number is on the block list`() {
        assertEquals(NumberStatus.BLOCKED, statusOf("+15550000000", setOf("+15550000000"), emptySet()))
    }

    @Test
    fun `statusOf reports ALLOWED when the number is on the allow list`() {
        assertEquals(NumberStatus.ALLOWED, statusOf("+15550000000", emptySet(), setOf("+15550000000")))
    }

    @Test
    fun `statusOf reports NONE when the number is on neither list`() {
        assertEquals(NumberStatus.NONE, statusOf("+15550000000", emptySet(), emptySet()))
    }

    @Test
    fun `statusOf prefers BLOCKED over ALLOWED when a number is somehow on both lists`() {
        assertEquals(
            NumberStatus.BLOCKED,
            statusOf("+15550000000", setOf("+15550000000"), setOf("+15550000000")),
        )
    }

    @Test
    fun `callOutcomeLabel reports Blocked when the call matches the merged blocked-calls log`() {
        val call =
            RecentCall(number = "+15550000000", name = null, timestamp = 1_000L, type = CallLog.Calls.MISSED_TYPE)
        val blockedCalls =
            listOf(UnifiedBlockedCall("+15550000000", null, 1_000L, "On your block list", "Call Manager"))

        assertEquals("Blocked", callOutcomeLabel(call, blockedCalls))
    }

    @Test
    fun `callOutcomeLabel falls back to the system call type label when the call was not blocked`() {
        val call =
            RecentCall(number = "+15550000000", name = null, timestamp = 1_000L, type = CallLog.Calls.MISSED_TYPE)

        assertEquals("Missed", callOutcomeLabel(call, emptyList()))
    }
}
