package com.matheus.callmanager.feature.recent

import android.provider.CallLog
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

private const val NUMBER_A = "+15550000001"
private const val NUMBER_B = "+15550000002"

private fun call(
    number: String = NUMBER_A,
    name: String? = null,
    timestamp: Long = 1_000_000L,
    type: Int = CallLog.Calls.INCOMING_TYPE,
) = RecentCall(number = number, name = name, timestamp = timestamp, type = type)

private fun blockedAt(
    number: String,
    timestamp: Long,
) = UnifiedBlockedCall(number, null, timestamp, "On your block list", "Call Manager")

class RecentFilteringTest {
    private val mixed =
        listOf(
            call(NUMBER_A, timestamp = 5_000_000L, type = CallLog.Calls.INCOMING_TYPE),
            call(NUMBER_B, timestamp = 4_000_000L, type = CallLog.Calls.MISSED_TYPE),
            call(NUMBER_A, timestamp = 3_000_000L, type = CallLog.Calls.BLOCKED_TYPE),
            call(NUMBER_B, timestamp = 2_000_000L, type = CallLog.Calls.OUTGOING_TYPE),
            call(NUMBER_A, timestamp = 1_000_000L, type = CallLog.Calls.REJECTED_TYPE),
        )

    @Test
    fun `ALL keeps every call`() {
        assertEquals(mixed, mixed.filterCalls(RecentFilter.ALL, query = "", blockedCalls = emptyList()))
    }

    @Test
    fun `BLOCKED keeps system-blocked and rejected calls`() {
        val result = mixed.filterCalls(RecentFilter.BLOCKED, query = "", blockedCalls = emptyList())

        assertEquals(listOf(3_000_000L, 1_000_000L), result.map { it.timestamp })
    }

    @Test
    fun `BLOCKED includes a silenced call that only our own log knows was blocked`() {
        val silenced = call(NUMBER_B, timestamp = 4_000_000L, type = CallLog.Calls.MISSED_TYPE)

        val result =
            listOf(silenced).filterCalls(
                RecentFilter.BLOCKED,
                query = "",
                blockedCalls = listOf(blockedAt(NUMBER_B, 4_000_000L)),
            )

        assertEquals(listOf(silenced), result)
    }

    @Test
    fun `MISSED excludes a missed call that was actually blocked`() {
        val silenced = call(NUMBER_B, timestamp = 4_000_000L, type = CallLog.Calls.MISSED_TYPE)

        val result =
            listOf(silenced).filterCalls(
                RecentFilter.MISSED,
                query = "",
                blockedCalls = listOf(blockedAt(NUMBER_B, 4_000_000L)),
            )

        assertEquals(emptyList<RecentCall>(), result)
    }

    @Test
    fun `MISSED keeps only missed calls`() {
        val result = mixed.filterCalls(RecentFilter.MISSED, query = "", blockedCalls = emptyList())

        assertEquals(listOf(4_000_000L), result.map { it.timestamp })
    }

    @Test
    fun `ANSWERED keeps incoming calls that were picked up`() {
        val result = mixed.filterCalls(RecentFilter.ANSWERED, query = "", blockedCalls = emptyList())

        assertEquals(listOf(5_000_000L), result.map { it.timestamp })
    }

    @Test
    fun `search matches a contact name ignoring case`() {
        val calls = listOf(call(NUMBER_A, name = "Alice Smith"), call(NUMBER_B, name = "Bob"))

        val result = calls.filterCalls(RecentFilter.ALL, query = "aLiCe", blockedCalls = emptyList())

        assertEquals(listOf("Alice Smith"), result.map { it.name })
    }

    @Test
    fun `search matches digits regardless of number formatting`() {
        val calls = listOf(call(NUMBER_A), call(NUMBER_B))

        val result = calls.filterCalls(RecentFilter.ALL, query = "(555) 000-0002", blockedCalls = emptyList())

        assertEquals(listOf(NUMBER_B), result.map { it.number })
    }

    @Test
    fun `a query with no digits does not match every number`() {
        val calls = listOf(call(NUMBER_A, name = null))

        val result = calls.filterCalls(RecentFilter.ALL, query = "zzz", blockedCalls = emptyList())

        assertEquals(emptyList<RecentCall>(), result)
    }

    @Test
    fun `blank query applies only the filter`() {
        val result = mixed.filterCalls(RecentFilter.MISSED, query = "   ", blockedCalls = emptyList())

        assertEquals(1, result.size)
    }

    @Test
    fun `groupByDay buckets calls by local date, newest day first`() {
        val dayMillis = 86_400_000L
        val calls =
            listOf(
                call(NUMBER_A, timestamp = 2 * dayMillis + 10),
                call(NUMBER_B, timestamp = 2 * dayMillis + 5),
                call(NUMBER_A, timestamp = 1 * dayMillis + 7),
            )

        val groups = calls.groupByDay(ZoneOffset.UTC)

        assertEquals(listOf(LocalDate.of(1970, 1, 3), LocalDate.of(1970, 1, 2)), groups.map { it.date })
        assertEquals(listOf(2, 1), groups.map { it.groups.size })
    }

    @Test
    fun `groupByDay groups calls from the same number within a day, newest number first`() {
        val calls =
            listOf(
                call(NUMBER_A, name = "Alice", timestamp = 1_000L),
                call(NUMBER_B, timestamp = 3_000L),
                call(NUMBER_A, timestamp = 2_000L),
            )

        val day = calls.groupByDay(ZoneOffset.UTC).single()

        assertEquals(listOf(NUMBER_B, NUMBER_A), day.groups.map { it.number })
        assertEquals(
            listOf(2_000L, 1_000L),
            day.groups
                .last()
                .calls
                .map { it.timestamp },
        )
        assertEquals("Alice", day.groups.last().name)
    }

    @Test
    fun `a number that called on two days appears under each day`() {
        val dayMillis = 86_400_000L
        val calls = listOf(call(NUMBER_A, timestamp = dayMillis + 1), call(NUMBER_A, timestamp = 1))

        val groups = calls.groupByDay(ZoneOffset.UTC)

        assertEquals(
            listOf(1, 1),
            groups.map {
                it.groups
                    .single()
                    .calls.size
            },
        )
    }

    @Test
    fun `groupByDay respects the zone when deciding where midnight falls`() {
        val oneAmUtc = call(timestamp = 3_600_000L)

        val groups = listOf(oneAmUtc).groupByDay(ZoneOffset.ofHours(-5))

        assertEquals(LocalDate.of(1969, 12, 31), groups.single().date)
    }

    @Test
    fun `groupByDay of nothing is empty`() {
        assertEquals(emptyList<DayGroup>(), emptyList<RecentCall>().groupByDay(ZoneOffset.UTC))
    }
}
