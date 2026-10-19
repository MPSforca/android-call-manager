package com.matheus.callmanager.feature.recent

import android.provider.CallLog
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import com.matheus.callmanager.testing.ComposeHostActivityRule
import com.matheus.callmanager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

private const val ALICE = "+15550000001"
private const val BOB = "+15550000002"
private val TODAY = LocalDate.of(2026, 10, 18)
private val NOON_TODAY = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
private val NOON_YESTERDAY =
    TODAY
        .minusDays(1)
        .atTime(12, 0)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentContentTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private fun show(
        state: RecentUiState,
        onFilterSelected: (RecentFilter) -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onClearFilters: () -> Unit = {},
        onCallSelected: (String) -> Unit = {},
        onLoadMore: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme {
                RecentContent(
                    state = state,
                    today = TODAY,
                    zone = ZoneOffset.UTC,
                    actions =
                        RecentActions(
                            onFilterSelected = onFilterSelected,
                            onQueryChange = onQueryChange,
                            onClearFilters = onClearFilters,
                            onCallSelected = onCallSelected,
                            onLoadMore = onLoadMore,
                        ),
                )
            }
        }
    }

    private fun call(
        number: String,
        name: String?,
        timestamp: Long,
        type: Int = CallLog.Calls.MISSED_TYPE,
    ) = RecentCall(number, name, timestamp, type)

    @Test
    fun `groups calls under Today and Yesterday headers`() {
        show(
            RecentUiState(
                calls =
                    listOf(
                        call(ALICE, "Alice", NOON_TODAY),
                        call(BOB, "Bob", NOON_YESTERDAY),
                    ),
            ),
        )

        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Yesterday").assertExists()
        compose.onNodeWithText("Alice").assertExists()
        compose.onNodeWithText("Bob").assertExists()
    }

    @Test
    fun `older days show a formatted date header`() {
        val older =
            TODAY
                .minusDays(10)
                .atTime(12, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()

        show(RecentUiState(calls = listOf(call(ALICE, "Alice", older))))

        compose.onNodeWithText("Today").assertDoesNotExist()
        compose.onNodeWithText("Yesterday").assertDoesNotExist()
        compose.onNodeWithText(formatDayHeader(TODAY.minusDays(10))).assertExists()
    }

    @Test
    fun `one row per number within a day, with the call count`() {
        show(
            RecentUiState(
                calls =
                    listOf(
                        call(ALICE, "Alice", NOON_TODAY),
                        call(ALICE, "Alice", NOON_TODAY - 60_000),
                        call(ALICE, "Alice", NOON_TODAY - 120_000),
                    ),
            ),
        )

        compose.onNodeWithText("Alice").assertExists()
        compose.onNodeWithText("3 calls", substring = true).assertExists()
    }

    @Test
    fun `a single call shows no count`() {
        show(RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY))))

        compose.onNodeWithText("1 call", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a call recorded in our own blocked log is labelled Blocked`() {
        show(
            RecentUiState(
                calls = listOf(call(ALICE, "Alice", NOON_TODAY)),
                blockedCalls = listOf(UnifiedBlockedCall(ALICE, null, NOON_TODAY, "r", "Call Manager")),
            ),
        )

        // One "Blocked" is the filter chip, the other is the row's outcome pill.
        compose.onAllNodesWithText("Blocked").assertCountEquals(2)
    }

    @Test
    fun `tapping a row reports its number`() {
        var selected: String? = null
        show(RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY))), onCallSelected = { selected = it })

        compose.onNodeWithText("Alice").performClick()

        assertEquals(ALICE, selected)
    }

    @Test
    fun `selecting a chip reports the filter`() {
        var chosen: RecentFilter? = null
        show(
            RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY, CallLog.Calls.INCOMING_TYPE))),
            onFilterSelected = { chosen = it },
        )

        compose.onNodeWithText("Missed").performClick()

        assertEquals(RecentFilter.MISSED, chosen)
    }

    @Test
    fun `typing in the search field reports the query`() {
        var query: String? = null
        show(RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY))), onQueryChange = { query = it })

        compose.onNodeWithText("Search name or number").performTextInput("ali")

        assertEquals("ali", query)
    }

    @Test
    fun `a non-empty query can be cleared from the field`() {
        var query: String? = null
        show(RecentUiState(query = "ali", calls = emptyList()), onQueryChange = { query = it })

        compose.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", query)
    }

    @Test
    fun `an empty log shows the empty state`() {
        show(RecentUiState())

        compose.onNodeWithText("No calls yet").assertExists()
    }

    @Test
    fun `no matches while filtering offers to clear the filters`() {
        var cleared = false
        show(RecentUiState(filter = RecentFilter.BLOCKED), onClearFilters = { cleared = true })

        compose.onNodeWithText("No matching calls").assertExists()
        compose.onNodeWithText("Clear filters").performClick()

        assertEquals(true, cleared)
    }

    @Test
    fun `load more appears when there are more pages and requests the next one`() {
        var loaded = false
        show(
            RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY)), hasMore = true),
            onLoadMore = { loaded = true },
        )

        compose.onNodeWithText("Load more").performClick()

        assertEquals(true, loaded)
    }

    @Test
    fun `load more is hidden on the last page`() {
        show(RecentUiState(calls = listOf(call(ALICE, "Alice", NOON_TODAY)), hasMore = false))

        compose.onNodeWithText("Load more").assertDoesNotExist()
    }
}
