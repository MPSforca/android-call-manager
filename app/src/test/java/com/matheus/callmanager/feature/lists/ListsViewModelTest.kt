package com.matheus.callmanager.feature.lists

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
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
class ListsViewModelTest {
    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var repository: CallRepository
    private lateinit var viewModel: ListsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        viewModel = ListsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `blockContact adds the number to the block list`() =
        runTest {
            viewModel.uiState.map { it.blocked }.test {
                assertEquals(0, awaitItem().size)
                viewModel.blockContact("+15550000000", "Spammer")
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `unblock removes a block-list entry`() =
        runTest {
            viewModel.uiState.map { it.blocked }.test {
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
            viewModel.uiState.map { it.allowed }.test {
                assertEquals(0, awaitItem().size)
                viewModel.allowContact("+15550000000", "Friend")
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `unallow removes an allow-list entry`() =
        runTest {
            viewModel.uiState.map { it.allowed }.test {
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
            viewModel.uiState.map { it.namePatterns }.test {
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
            viewModel.uiState.map { it.namePatterns }.test {
                assertEquals(0, awaitItem().size)
                viewModel.addAllowedNamePattern("PagerDuty")
                val entry = awaitItem().first()

                viewModel.removeAllowedNamePattern(entry)

                assertEquals(0, awaitItem().size)
            }
        }

    private suspend fun ReceiveTurbine<ListsUiState>.awaitState(predicate: (ListsUiState) -> Boolean): ListsUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun `search filters the lists by name or number and reports the unfiltered total`() =
        runTest {
            viewModel.blockContact("+15550000001", "Spammer")
            viewModel.blockContact("+15550000002", "Telemarketer")
            viewModel.uiState.test {
                awaitState { it.blocked.size == 2 }

                viewModel.setQuery("spam")
                val byName = awaitState { it.query == "spam" }
                assertEquals(listOf("+15550000001"), byName.blocked.map { it.number })
                assertEquals(2, byName.totalInTab)

                viewModel.setQuery("0002")
                assertEquals(listOf("+15550000002"), awaitState { it.query == "0002" }.blocked.map { it.number })

                viewModel.setQuery("nobody")
                assertEquals(0, awaitState { it.query == "nobody" }.blocked.size)
            }
        }

    @Test
    fun `search also filters allowed numbers and name patterns`() =
        runTest {
            viewModel.allowContact("+15550000003", "Friend")
            viewModel.allowContact("+15550000004", "Colleague")
            viewModel.addAllowedNamePattern("PagerDuty")
            viewModel.addAllowedNamePattern("Doctor")
            viewModel.uiState.test {
                awaitState { it.allowed.size == 2 && it.namePatterns.size == 2 }

                viewModel.setQuery("friend")
                assertEquals(1, awaitState { it.query == "friend" }.allowed.size)

                viewModel.setQuery("pager")
                assertEquals(listOf("PagerDuty"), awaitState { it.query == "pager" }.namePatterns.map { it.pattern })
            }
        }

    @Test
    fun `selecting a tab switches the total and clears the query`() =
        runTest {
            viewModel.blockContact("+15550000001", null)
            viewModel.addAllowedNamePattern("PagerDuty")
            viewModel.addAllowedNamePattern("Doctor")
            viewModel.uiState.test {
                awaitState { it.blocked.size == 1 && it.namePatterns.size == 2 }
                viewModel.setQuery("x")
                awaitState { it.query == "x" }

                viewModel.selectTab(ListTab.NamePatterns)
                val state = awaitState { it.tab == ListTab.NamePatterns }
                assertEquals("", state.query)
                assertEquals(2, state.totalInTab)

                viewModel.selectTab(ListTab.Allowed)
                assertEquals(0, awaitState { it.tab == ListTab.Allowed }.totalInTab)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
