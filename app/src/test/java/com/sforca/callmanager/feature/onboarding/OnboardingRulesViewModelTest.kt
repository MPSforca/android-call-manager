package com.sforca.callmanager.feature.onboarding

import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.testing.FakeSettingsRepository
import com.sforca.callmanager.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingRulesViewModelTest {
    @get:Rule
    val dispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    // Lazy: the view model captures Dispatchers.Main, which the rule only installs once the test starts.
    private val viewModel by lazy { OnboardingRulesViewModel(settings) }

    private fun needsSetup(): Boolean {
        var result: Boolean? = null
        viewModel.needsRulesSetup { result = it }
        return result!!
    }

    @Test
    fun `rules setup is needed on a fresh install`() {
        assertTrue(needsSetup())
    }

    @Test
    fun `finishing marks the setup done exactly once and notifies`() =
        runTest {
            var finished = false
            viewModel.finish { finished = true }

            assertTrue(finished)
            assertTrue(settings.preferences.first().rulesSetupCompleted)
            assertFalse(needsSetup())
        }

    @Test
    fun `choices are written straight to settings`() =
        runTest {
            viewModel.setBlockUnknownNumbers(true)
            viewModel.setBlockedCallAction(BlockedCallAction.REJECT)

            val prefs = settings.preferences.first()
            assertTrue(prefs.blockUnknownNumbers)
            assertEquals(BlockedCallAction.REJECT, prefs.blockedCallAction)
        }
}
