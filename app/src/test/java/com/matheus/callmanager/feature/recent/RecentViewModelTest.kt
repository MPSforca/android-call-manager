package com.matheus.callmanager.feature.recent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.datastore.SettingsStore
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.testing.FakeActivityRetainedLifecycle
import com.matheus.callmanager.testing.FakeCallLogDataSource
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var db: CallManagerDatabase
    private lateinit var repository: CallRepository
    private lateinit var callLogDataSource: FakeCallLogDataSource
    private lateinit var lifecycle: FakeActivityRetainedLifecycle
    private lateinit var viewModel: RecentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java).allowMainThreadQueries().build()
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), SettingsStore(context))
        callLogDataSource = FakeCallLogDataSource()
        lifecycle = FakeActivityRetainedLifecycle()
        val pager = SystemBlockedCallsPager(repository, callLogDataSource, Dispatchers.Default, lifecycle)
        viewModel = RecentViewModel(repository, callLogDataSource, pager, Dispatchers.Default)
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
}
