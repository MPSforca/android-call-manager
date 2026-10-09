package com.matheus.callmanager.feature.debug

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.datastore.SettingsStore
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugViewModelTest {
    private lateinit var db: CallManagerDatabase
    private lateinit var repository: CallRepository
    private lateinit var viewModel: DebugViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java).allowMainThreadQueries().build()
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), SettingsStore(context))
        viewModel = DebugViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `simulating an allowed call reports ALLOWED`() =
        runTest {
            viewModel.simulationResult.test {
                assertEquals(null, awaitItem())
                viewModel.simulateIncomingCall("+15550000000")
                assertTrue(awaitItem()!!.startsWith("ALLOWED"))
            }
        }

    @Test
    fun `simulating a rejected call reports REJECTED and logs it`() =
        runTest {
            repository.settings.blockUnknownNumbers = true

            viewModel.simulationResult.test {
                assertEquals(null, awaitItem())
                viewModel.simulateIncomingCall("+15550000001", "Spammer")
                assertTrue(awaitItem()!!.startsWith("REJECTED"))
            }

            val log = repository.observeBlockedCallLog().first()
            assertEquals(1, log.size)
            assertEquals("+15550000001", log.first().number)
        }

    @Test
    fun `clearing the result resets it to null`() =
        runTest {
            viewModel.simulationResult.test {
                assertEquals(null, awaitItem())
                viewModel.simulateIncomingCall("+15550000000")
                awaitItem()
                viewModel.clearSimulationResult()
                assertEquals(null, awaitItem())
            }
        }
}
