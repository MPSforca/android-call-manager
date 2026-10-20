package com.matheus.callmanager.feature.rules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.MainDispatcherRule
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RulesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: RulesViewModel
    private lateinit var repository: CallRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
        viewModel = RulesViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `uiState emits defaults and then follow the settings repository`() =
        runTest {
            viewModel.uiState.test {
                assertEquals(RulesUiState(), awaitItem())

                settings.update { copy(blockUnknownNumbers = true) }

                assertEquals(RulesUiState(UserPreferences(blockUnknownNumbers = true)), awaitItem())
            }
        }

    @Test
    fun `uiState counts the block list, allow list and name patterns`() =
        runTest {
            db.blockListDao().insertAll(listOf(BlockListEntry("+15550000001", "A", 1L)))
            db.allowListDao().insertAll(
                listOf(AllowListEntry("+15550000002", "B", 1L), AllowListEntry("+15550000003", "C", 1L)),
            )
            repository.addAllowedNamePattern("Dr.")

            viewModel.uiState.test {
                var item = awaitItem()
                while (item.listCounts != ListCounts(1, 2, 1)) item = awaitItem()
                assertEquals(ListCounts(1, 2, 1), item.listCounts)
            }
        }

    @Test
    fun `preview re-evaluates the number live as rules change and clears when blank`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.setPreview("+15550000009", "Someone")
                var item = awaitItem()
                while (item.preview == null) item = awaitItem()
                assertEquals(ScreeningDecision.Allow::class, item.preview!!.trace.decision::class)

                settings.update { copy(blockUnknownNumbers = true) }
                while (item.preview?.trace?.decision !is ScreeningDecision.Reject) item = awaitItem()
                assertEquals("+15550000009", item.preview!!.number)

                viewModel.setPreview("  ")
                while (item.preview != null) item = awaitItem()
                assertNull(item.preview)
            }
        }

    @Test
    fun `previewing never logs a blocked call`() =
        runTest {
            settings.update { copy(blockUnknownNumbers = true) }
            viewModel.uiState.test {
                viewModel.setPreview("+15550000009")
                var item = awaitItem()
                while (item.preview == null) item = awaitItem()
                assertEquals(0, repository.observeBlockedCallLog().first().size)
            }
        }

    @Test
    fun `every setter writes through to the settings repository`() =
        runTest {
            viewModel.setAllowContacts(false)
            viewModel.setRecentAcceptedRuleEnabled(false)
            viewModel.setAllowRecentAcceptedDays(7)
            viewModel.setRecentOutgoingRuleEnabled(false)
            viewModel.setAllowRecentOutgoingDays(9)
            viewModel.setBlockUnknownNumbers(true)
            viewModel.setBlockedCallAction(BlockedCallAction.REJECT)
            viewModel.setShowRejectedCallNotification(false)
            viewModel.setDynamicColor(true)

            viewModel.uiState.test {
                assertEquals(
                    RulesUiState(
                        UserPreferences(
                            allowContacts = false,
                            recentAcceptedRuleEnabled = false,
                            allowRecentAcceptedDays = 7,
                            recentOutgoingRuleEnabled = false,
                            allowRecentOutgoingDays = 9,
                            blockUnknownNumbers = true,
                            blockedCallAction = BlockedCallAction.REJECT,
                            showRejectedCallNotification = false,
                            dynamicColor = true,
                        ),
                    ),
                    expectMostRecentItem(),
                )
            }
        }
}
