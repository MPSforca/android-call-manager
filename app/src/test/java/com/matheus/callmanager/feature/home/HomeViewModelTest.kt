package com.matheus.callmanager.feature.home

import android.content.Context
import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.testing.FakeActivityRetainedLifecycle
import com.matheus.callmanager.testing.FakeCallLogDataSource
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: CallManagerDatabase
    private lateinit var callLogDataSource: FakeCallLogDataSource
    private lateinit var lifecycle: FakeActivityRetainedLifecycle
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = inMemoryTestDatabase(ApplicationProvider.getApplicationContext<Context>())
        val repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), FakeSettingsRepository())
        callLogDataSource = FakeCallLogDataSource()
        lifecycle = FakeActivityRetainedLifecycle()
        val pager = SystemBlockedCallsPager(repository, callLogDataSource, testDispatcher, lifecycle)
        viewModel = HomeViewModel(callLogDataSource, pager, testDispatcher)
    }

    @After
    fun tearDown() {
        lifecycle.triggerCleared()
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `refresh loads the latest calls`() =
        runTest {
            callLogDataSource.recentCallPages =
                listOf(listOf(RecentCall("+15550000001", "Alice", 1L, CallLog.Calls.MISSED_TYPE)))

            viewModel.calls.test {
                assertEquals(0, awaitItem().size)
                viewModel.refresh()
                assertEquals(listOf("Alice"), awaitItem().map { it.name })
            }
        }
}
