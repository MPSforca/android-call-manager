package com.matheus.callmanager.feature.debug

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.data.rule.TraceOutcome
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.MainDispatcherRule
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val HISTORY_CAP = 20

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var repository: CallRepository
    private lateinit var viewModel: DebugViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        viewModel = DebugViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Room runs queries on its own executor, so wait for the new newest history entry rather than assuming it is instant. */
    private fun simulate(
        number: String,
        callerName: String? = null,
    ) = runBlocking {
        viewModel.simulateIncomingCall(number, callerName)
        withTimeout(5_000) { viewModel.uiState.first { it.history.firstOrNull()?.number == number } }
    }

    @Test
    fun `starts with no trace and no history`() {
        assertNull(viewModel.uiState.value.trace)
        assertTrue(
            viewModel.uiState.value.history
                .isEmpty(),
        )
    }

    @Test
    fun `an allowed call carries a trace where only the fallback decided`() {
        simulate("+15550000000")

        val trace = viewModel.uiState.value.trace!!
        assertTrue(trace.decision is ScreeningDecision.Allow)
        assertEquals(ScreeningRuleId.entries, trace.steps.map { it.rule })
        assertEquals(TraceOutcome.DECIDED, trace.steps.last().outcome)
        assertTrue(trace.steps.dropLast(1).all { it.outcome == TraceOutcome.PASSED })
    }

    @Test
    fun `a rejected call is logged and later rules are not reached`() =
        runBlocking {
            repository.blockNumbers(listOf("+15550000001" to null))

            simulate("+15550000001", "Spammer")

            val trace = viewModel.uiState.value.trace!!
            assertTrue(trace.decision is ScreeningDecision.Reject)
            assertEquals(TraceOutcome.DECIDED, trace.steps.first().outcome)
            assertTrue(trace.steps.drop(1).all { it.outcome == TraceOutcome.NOT_REACHED })
            val log = repository.observeBlockedCallLog().first()
            assertEquals(listOf("+15550000001"), log.map { it.number })
        }

    @Test
    fun `history keeps newest first and is capped`() {
        repeat(25) { simulate("+1555000${it.toString().padStart(4, '0')}") }

        val history = viewModel.uiState.value.history
        assertEquals(HISTORY_CAP, history.size)
        assertEquals("+15550000024", history.first().number)
    }

    @Test
    fun `clearing history resets state`() {
        simulate("+15550000000")

        viewModel.clearHistory()

        assertEquals(DebugUiState(), viewModel.uiState.value)
    }
}
