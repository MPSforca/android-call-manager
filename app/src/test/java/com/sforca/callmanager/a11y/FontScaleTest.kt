package com.sforca.callmanager.a11y

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.feature.onboarding.OnboardingActions
import com.sforca.callmanager.feature.onboarding.OnboardingScreen
import com.sforca.callmanager.feature.onboarding.PermissionsState
import com.sforca.callmanager.testing.ComposeHostRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * At 200% font scale the longest (pt-BR / es) copy must stay reachable: every action has to scroll
 * into view and respond to a tap rather than being clipped off-screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FontScaleTest {
    @get:Rule(order = 0)
    val host = ComposeHostRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun showOnboarding(state: PermissionsState) =
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                CallManagerTheme {
                    OnboardingScreen(
                        state = state,
                        actions =
                            OnboardingActions(
                                onRequestRole = { calls += "role" },
                                onRequestPermissions = { calls += "permissions" },
                                onRequestBatteryExemption = { calls += "battery" },
                                onSkip = { calls += "skip" },
                                onContinue = { calls += "continue" },
                                onOpenPrivacy = { calls += "privacy" },
                            ),
                    )
                }
            }
        }

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun assertOnboardingReachable() {
        showOnboarding(PermissionsState())
        compose.onNodeWithText(string(R.string.onboarding_role_action)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.onboarding_skip)).performScrollTo().performClick()
        assertEquals(listOf("role", "skip"), calls)
    }

    @Test
    fun `onboarding actions stay reachable at 200 percent in english`() = assertOnboardingReachable()

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `onboarding actions stay reachable at 200 percent in portuguese`() = assertOnboardingReachable()

    @Test
    @Config(qualifiers = "es")
    fun `onboarding actions stay reachable at 200 percent in spanish`() = assertOnboardingReachable()
}
