package com.matheus.callmanager.feature.lists

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.core.telephony.CallLogUtil
import com.matheus.callmanager.testing.FakeActivityRetainedLifecycle
import com.matheus.callmanager.testing.FakeCallLogDataSource
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListsViewModelTest {
    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var repository: CallRepository
    private lateinit var callLogDataSource: FakeCallLogDataSource
    private lateinit var lifecycle: FakeActivityRetainedLifecycle
    private lateinit var viewModel: ListsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java).allowMainThreadQueries().build()
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        callLogDataSource = FakeCallLogDataSource()
        lifecycle = FakeActivityRetainedLifecycle()
        val pager = SystemBlockedCallsPager(repository, callLogDataSource, Dispatchers.Default, lifecycle)
        viewModel = ListsViewModel(repository, pager)
    }

    @After
    fun tearDown() {
        lifecycle.triggerCleared()
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `blockContact adds the number to the block list`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockContact("+15550000000", "Spammer")
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `unblock removes a block-list entry`() =
        runTest {
            viewModel.blockList.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockContact("+15550000000", "Spammer")
                val entry = awaitItem().first()

                viewModel.unblock(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `allowContact adds the number to the allow list`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowContact("+15550000000", "Friend")
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `unallow removes an allow-list entry`() =
        runTest {
            viewModel.allowList.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowContact("+15550000000", "Friend")
                val entry = awaitItem().first()

                viewModel.unallow(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `addAllowedNamePattern adds a pattern and ignores a blank one`() =
        runTest {
            viewModel.allowedNamePatterns.test {
                assertEquals(0, awaitItem().size)

                viewModel.addAllowedNamePattern("  ")
                expectNoEvents()

                viewModel.addAllowedNamePattern("PagerDuty")
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `removeAllowedNamePattern removes a pattern`() =
        runTest {
            viewModel.allowedNamePatterns.test {
                assertEquals(0, awaitItem().size)
                viewModel.addAllowedNamePattern("PagerDuty")
                val entry = awaitItem().first()

                viewModel.removeAllowedNamePattern(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    @Test
    fun `refreshBlockedCalls and loadMoreBlockedCalls delegate to the shared pager`() =
        runTest {
            callLogDataSource.blockedCallPages =
                listOf(
                    List(CallLogUtil.DEFAULT_PAGE_SIZE) { SystemBlockedCall("+1555111$it", null, it.toLong(), "Test") },
                    List(2) { SystemBlockedCall("+1555222$it", null, it.toLong(), "Test") },
                )

            viewModel.blockedCalls.test {
                assertEquals(0, awaitItem().size)
                viewModel.refreshBlockedCalls()
                assertEquals(CallLogUtil.DEFAULT_PAGE_SIZE, awaitItem().size)
                viewModel.loadMoreBlockedCalls()
                assertEquals(CallLogUtil.DEFAULT_PAGE_SIZE + 2, awaitItem().size)
            }
            assertFalse(viewModel.hasMoreBlockedCalls.value)
        }
}
