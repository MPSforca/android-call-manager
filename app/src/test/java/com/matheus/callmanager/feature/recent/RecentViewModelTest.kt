package com.matheus.callmanager.feature.recent

import android.content.Context
import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.entity.BlockedCallLog
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.testing.FakeActivityRetainedLifecycle
import com.matheus.callmanager.testing.FakeCallLogDataSource
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun fakeRecentCall(index: Int) =
    RecentCall(number = "+1555000$index", name = null, timestamp = index.toLong(), type = 1)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var repository: CallRepository
    private lateinit var callLogDataSource: FakeCallLogDataSource
    private lateinit var lifecycle: FakeActivityRetainedLifecycle
    private lateinit var viewModel: RecentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        callLogDataSource = FakeCallLogDataSource()
        lifecycle = FakeActivityRetainedLifecycle()
        val pager = SystemBlockedCallsPager(repository, callLogDataSource, testDispatcher, lifecycle)
        viewModel = RecentViewModel(repository, callLogDataSource, pager, testDispatcher)
    }

    @After
    fun tearDown() {
        lifecycle.triggerCleared()
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `refreshing loads the first page and flags more when it is full`() =
        runTest {
            callLogDataSource.recentCallPages = listOf(List(200) { fakeRecentCall(it) })

            viewModel.recentCalls.test {
                assertEquals(0, awaitItem().size)
                viewModel.refreshRecentCalls()
                assertEquals(200, awaitItem().size)
            }
            assertTrue(viewModel.hasMoreRecentCalls.value)
        }

    @Test
    fun `loading more appends the next page and stops flagging more once it is short`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(List(200) { fakeRecentCall(it) }, List(3) { fakeRecentCall(200 + it) })

            viewModel.recentCalls.test {
                assertEquals(0, awaitItem().size)
                viewModel.refreshRecentCalls()
                assertEquals(200, awaitItem().size)
                viewModel.loadMoreRecentCalls()
                assertEquals(203, awaitItem().size)
            }
            assertFalse(viewModel.hasMoreRecentCalls.value)
        }

    @Test
    fun `blockNumbers adds the numbers to the block list`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockNumbers(setOf("+15550000000"))
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `blocking a number removes any conflicting allow-list entry`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowNumbers(setOf("+15550000000"))
                assertEquals(1, awaitItem().size)

                viewModel.blockNumbers(setOf("+15550000000"))

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `unblock removes an entry from the block list`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockNumbers(setOf("+15550000000"))
                val entry = awaitItem().first()

                viewModel.unblock(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `blockNumber adds one number with its name`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)

                viewModel.blockNumber("+15550000000", "Spammer")

                val entry = awaitItem().single()
                assertEquals("+15550000000", entry.number)
                assertEquals("Spammer", entry.displayName)
            }
        }

    @Test
    fun `undoBlock removes the block entry`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockNumber("+15550000000", null)
                assertEquals(1, awaitItem().size)

                viewModel.undoBlock("+15550000000", null, restoreAllow = false)

                assertEquals(0, awaitItem().size)
            }
            assertEquals(0, viewModel.allowList.value.size)
        }

    @Test
    fun `undoBlock puts a displaced allow entry back`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowNumbers(setOf("+15550000000"))
                assertEquals(1, awaitItem().size)
                viewModel.blockNumber("+15550000000", null)
                assertEquals(0, awaitItem().size)

                viewModel.undoBlock("+15550000000", null, restoreAllow = true)

                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `toggleBlock adds then removes a single number from the block list`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)

                viewModel.toggleBlock("+15550000000", "Spammer")
                assertEquals(1, awaitItem().size)

                viewModel.toggleBlock("+15550000000", "Spammer")
                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `toggleAllow adds then removes a single number from the allow list`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)

                viewModel.toggleAllow("+15550000000", "Friend")
                assertEquals(1, awaitItem().size)

                viewModel.toggleAllow("+15550000000", "Friend")
                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `unallow removes an entry from the allow list`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowNumbers(setOf("+15550000000"))
                val entry = awaitItem().first()

                viewModel.unallow(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `refreshBlockedCalls delegates to the shared pager`() =
        runTest {
            callLogDataSource.blockedCallPages =
                listOf(List(3) { SystemBlockedCall("+1555111$it", null, it.toLong(), "Test") })

            viewModel.blockedCalls.test {
                assertEquals(0, awaitItem().size)
                viewModel.refreshBlockedCalls()
                assertEquals(3, awaitItem().size)
            }
        }

    @Test
    fun `visibleCalls starts unfiltered and narrows by filter chip`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(
                    listOf(
                        RecentCall("+15550000001", null, 2L, CallLog.Calls.MISSED_TYPE),
                        RecentCall("+15550000002", null, 1L, CallLog.Calls.INCOMING_TYPE),
                    ),
                )
            viewModel.refreshRecentCalls()
            viewModel.recentCalls.first { it.isNotEmpty() }

            viewModel.visibleCalls.test {
                assertEquals(2, expectMostRecentItem().size)
                viewModel.setFilter(RecentFilter.MISSED)
                assertEquals(listOf("+15550000001"), awaitItem().map { it.number })
            }
            assertEquals(RecentFilter.MISSED, viewModel.filter.value)
        }

    @Test
    fun `visibleCalls narrows by search query`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(
                    listOf(
                        RecentCall("+15550000001", "Alice", 2L, CallLog.Calls.MISSED_TYPE),
                        RecentCall("+15550000002", "Bob", 1L, CallLog.Calls.MISSED_TYPE),
                    ),
                )
            viewModel.refreshRecentCalls()
            viewModel.recentCalls.first { it.isNotEmpty() }

            viewModel.visibleCalls.test {
                assertEquals(2, expectMostRecentItem().size)
                viewModel.setQuery("bob")
                assertEquals(listOf("Bob"), awaitItem().map { it.name })
            }
            assertEquals("bob", viewModel.query.value)
        }

    @Test
    fun `a call blocked by our own log shows under the Blocked filter`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(listOf(RecentCall("+15550000001", null, 5_000L, CallLog.Calls.MISSED_TYPE)))
            viewModel.refreshRecentCalls()
            viewModel.recentCalls.first { it.isNotEmpty() }
            db.blockedCallLogDao().insert(
                BlockedCallLog(number = "+15550000001", callerName = null, timestamp = 5_000L, reason = "r"),
            )
            viewModel.setFilter(RecentFilter.BLOCKED)

            viewModel.visibleCalls.test {
                assertEquals(1, expectMostRecentItem().size)
            }
        }

    @Test
    fun `uiState combines the filtered calls, list membership and paging`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(
                    listOf(
                        RecentCall("+15550000001", "Alice", 2L, CallLog.Calls.MISSED_TYPE),
                        RecentCall("+15550000002", null, 1L, CallLog.Calls.INCOMING_TYPE),
                    ),
                )
            viewModel.uiState.test {
                viewModel.refreshRecentCalls()
                viewModel.blockNumbers(setOf("+15550000002"))
                viewModel.allowNumbers(setOf("+15550000003"))
                viewModel.setFilter(RecentFilter.MISSED)
                viewModel.setQuery("ali")

                val state =
                    awaitUntil {
                        it.filter == RecentFilter.MISSED &&
                            it.query == "ali" &&
                            it.calls.size == 1 &&
                            it.blockedNumbers.isNotEmpty() &&
                            it.allowedNumbers.isNotEmpty()
                    }

                assertEquals(RecentFilter.MISSED, state.filter)
                assertEquals("ali", state.query)
                assertEquals(listOf("Alice"), state.calls.map { it.name })
                assertEquals(setOf("+15550000002"), state.blockedNumbers)
                assertEquals(setOf("+15550000003"), state.allowedNumbers)
                assertFalse(state.hasMore)
                assertTrue(state.isFiltering)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState is not filtering by default`() =
        runTest {
            viewModel.uiState.test {
                assertFalse(awaitItem().isFiltering)
            }
        }

    @Test
    fun `clearFilters resets the chip and the query`() =
        runTest {
            viewModel.setFilter(RecentFilter.BLOCKED)
            viewModel.setQuery("x")

            viewModel.clearFilters()

            assertEquals(RecentFilter.ALL, viewModel.filter.value)
            assertEquals("", viewModel.query.value)
        }
}

private suspend fun ReceiveTurbine<RecentUiState>.awaitUntil(predicate: (RecentUiState) -> Boolean): RecentUiState {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
