package com.matheus.callmanager.feature.rules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.MainDispatcherRule
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        val repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings)
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
