package com.sforca.callmanager.core.data

import com.sforca.callmanager.core.database.entity.BlockedCallLog
import com.sforca.callmanager.core.model.SystemBlockedCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedCallMergerTest {
    @Test
    fun `own log entries are labeled as Call Manager`() {
        val own =
            listOf(
                BlockedCallLog(
                    number = "+15551111111",
                    callerName = "Spammer",
                    timestamp = 1_000,
                    reason = "On your block list",
                ),
            )

        val merged = BlockedCallMerger.merge(own, emptyList())

        assertEquals(1, merged.size)
        assertEquals("Call Manager", merged.first().source)
        assertEquals("On your block list", merged.first().reason)
    }

    @Test
    fun `system-only entries are labeled by their block reason`() {
        val system =
            listOf(
                SystemBlockedCall(
                    number = "+15552222222",
                    name = null,
                    timestamp = 2_000,
                    source = "Android's block list",
                ),
            )

        val merged = BlockedCallMerger.merge(emptyList(), system)

        assertEquals(1, merged.size)
        assertEquals("Android", merged.first().source)
        assertEquals("Android's block list", merged.first().reason)
    }

    @Test
    fun `a call that appears in both logs within the dedupe window is shown once, preferring the app entry`() {
        val own =
            listOf(
                BlockedCallLog(
                    number = "+15553333333",
                    callerName = null,
                    timestamp = 10_000,
                    reason = "On your block list",
                ),
            )
        val system =
            listOf(
                SystemBlockedCall(
                    number = "+15553333333",
                    name = null,
                    timestamp = 10_500,
                    source = "A call-screening app",
                ),
            )

        val merged = BlockedCallMerger.merge(own, system)

        assertEquals(1, merged.size)
        assertEquals("Call Manager", merged.first().source)
        assertEquals("On your block list", merged.first().reason)
    }

    @Test
    fun `a call from the same number far apart in time is kept as two separate entries`() {
        val own =
            listOf(
                BlockedCallLog(
                    number = "+15554444444",
                    callerName = null,
                    timestamp = 0,
                    reason = "On your block list",
                ),
            )
        val system =
            listOf(
                SystemBlockedCall(
                    number = "+15554444444",
                    name = null,
                    timestamp = 60_000,
                    source = "Android's block list",
                ),
            )

        val merged = BlockedCallMerger.merge(own, system)

        assertEquals(2, merged.size)
    }

    @Test
    fun `a call logged with different number formatting still dedupes`() {
        val own =
            listOf(
                BlockedCallLog(
                    number = "+15557777777",
                    callerName = null,
                    timestamp = 20_000,
                    reason = "On your block list",
                ),
            )
        val system =
            listOf(
                SystemBlockedCall(
                    number = "5557777777",
                    name = null,
                    timestamp = 20_200,
                    source = "A call-screening app",
                ),
            )

        val merged = BlockedCallMerger.merge(own, system)

        assertEquals(1, merged.size)
        assertEquals("Call Manager", merged.first().source)
    }

    @Test
    fun `numbers that share a suffix but differ in area code are not deduped`() {
        val own =
            listOf(
                BlockedCallLog(
                    number = "+14155557777",
                    callerName = null,
                    timestamp = 30_000,
                    reason = "On your block list",
                ),
            )
        val system =
            listOf(
                SystemBlockedCall(
                    number = "+12125557777",
                    name = null,
                    timestamp = 30_200,
                    source = "A call-screening app",
                ),
            )

        val merged = BlockedCallMerger.merge(own, system)

        assertEquals(2, merged.size)
    }

    @Test
    fun `wasBlocked matches a silenced call that the system never tagged BLOCKED_TYPE`() {
        val blocked =
            listOf(
                UnifiedBlockedCall(
                    number = "+15558888888",
                    name = null,
                    timestamp = 40_000,
                    reason = "On your block list",
                    source = "Call Manager",
                ),
            )

        assertEquals(true, BlockedCallMerger.wasBlocked("+15558888888", 40_100, blocked))
    }

    @Test
    fun `wasBlocked ignores calls far apart in time or from a different number`() {
        val blocked =
            listOf(
                UnifiedBlockedCall(
                    number = "+15558888888",
                    name = null,
                    timestamp = 40_000,
                    reason = "On your block list",
                    source = "Call Manager",
                ),
            )

        assertEquals(false, BlockedCallMerger.wasBlocked("+15558888888", 100_000, blocked))
        assertEquals(false, BlockedCallMerger.wasBlocked("+15559999999", 40_100, blocked))
    }

    @Test
    fun `results are sorted most recent first`() {
        val own =
            listOf(BlockedCallLog(number = "+15555555555", callerName = null, timestamp = 1_000, reason = "reason"))
        val system =
            listOf(
                SystemBlockedCall(
                    number = "+15556666666",
                    name = null,
                    timestamp = 2_000,
                    source = "Android's block list",
                ),
            )

        val merged = BlockedCallMerger.merge(own, system)

        assertEquals("+15556666666", merged.first().number)
        assertEquals("+15555555555", merged.last().number)
    }
}
