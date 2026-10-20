package com.sforca.callmanager.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.sforca.callmanager.core.database.CallManagerDatabase
import com.sforca.callmanager.core.model.SystemBlockedCall
import com.sforca.callmanager.core.telephony.CallLogUtil
import com.sforca.callmanager.testing.FakeActivityRetainedLifecycle
import com.sforca.callmanager.testing.FakeCallLogDataSource
import com.sforca.callmanager.testing.FakeContactsLookup
import com.sforca.callmanager.testing.FakeRecentOutgoingLookup
import com.sforca.callmanager.testing.FakeSettingsRepository
import com.sforca.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun fakeSystemBlockedCall(index: Int) =
    SystemBlockedCall(number = "+1555000$index", name = null, timestamp = index.toLong(), source = "Test")

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemBlockedCallsPagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var repository: CallRepository
    private lateinit var callLogDataSource: FakeCallLogDataSource
    private lateinit var lifecycle: FakeActivityRetainedLifecycle
    private lateinit var pager: SystemBlockedCallsPager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        callLogDataSource = FakeCallLogDataSource()
        lifecycle = FakeActivityRetainedLifecycle()
        pager = SystemBlockedCallsPager(repository, callLogDataSource, testDispatcher, lifecycle)
    }

    @After
    fun tearDown() {
        lifecycle.triggerCleared()
        db.close()
    }

    @Test
    fun `refresh with a short page marks hasMoreBlockedCalls false`() =
        runTest {
            callLogDataSource.blockedCallPages = listOf(List(3) { fakeSystemBlockedCall(it) })

            pager.hasMoreBlockedCalls.test {
                assertTrue(awaitItem())
                pager.refresh()
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `refresh with a full page keeps hasMoreBlockedCalls true and loadMore appends the next page`() =
        runTest {
            callLogDataSource.blockedCallPages =
                listOf(
                    List(CallLogUtil.DEFAULT_PAGE_SIZE) { fakeSystemBlockedCall(it) },
                    List(3) { fakeSystemBlockedCall(CallLogUtil.DEFAULT_PAGE_SIZE + it) },
                )

            pager.blockedCalls.test {
                assertEquals(0, awaitItem().size)
                pager.refresh()
                assertEquals(CallLogUtil.DEFAULT_PAGE_SIZE, awaitItem().size)
                pager.loadMore()
                assertEquals(CallLogUtil.DEFAULT_PAGE_SIZE + 3, awaitItem().size)
            }
            assertFalse(pager.hasMoreBlockedCalls.value)
        }

    @Test
    fun `loadMore is a no-op once hasMoreBlockedCalls is false`() =
        runTest {
            callLogDataSource.blockedCallPages = listOf(List(3) { fakeSystemBlockedCall(it) })

            pager.blockedCalls.test {
                assertEquals(0, awaitItem().size)
                pager.refresh()
                assertEquals(3, awaitItem().size)

                pager.loadMore()

                expectNoEvents()
            }
            assertEquals(1, callLogDataSource.systemBlockedCallsCallCount)
        }

    @Test
    fun `merges own blocked-call log with the system call log, deduping the same call`() =
        runTest {
            pager.blockedCalls.test {
                assertEquals(0, awaitItem().size)

                repository.blockNumbers(listOf("+15550000000" to "Spammer"))
                repository.logBlockedCall("+15550000000", "On your block list")
                val afterOwnLog = awaitItem()
                assertEquals(1, afterOwnLog.size)
                assertEquals("Call Manager", afterOwnLog.first().source)

                val now = afterOwnLog.first().timestamp
                callLogDataSource.blockedCallPages =
                    listOf(
                        listOf(
                            SystemBlockedCall("+15550000000", null, now, "A call-screening app"),
                            SystemBlockedCall("+15559999999", "Other Spammer", now, "Android's block list"),
                        ),
                    )
                pager.refresh()

                val merged = awaitItem()
                assertEquals(2, merged.size)
                assertTrue(merged.any { it.number == "+15550000000" && it.source == "Call Manager" })
                assertTrue(merged.any { it.number == "+15559999999" && it.source == "Android" })
            }
        }
}
