package com.sforca.callmanager.feature.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sforca.callmanager.testing.ComposeHostRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenTest {
    @get:Rule(order = 0)
    val host = ComposeHostRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: PermissionsState) =
        compose.setContent {
            OnboardingScreen(
                state = state,
                onRequestRole = { calls += "role" },
                onRequestPermissions = { calls += "permissions" },
                onRequestBatteryExemption = { calls += "battery" },
                onSkip = { calls += "skip" },
                onContinue = { calls += "continue" },
            )
        }

    @Test
    fun `every permission is requested from the same page`() {
        show(PermissionsState())

        compose.onNodeWithText("Set as call-screening app").performScrollTo().performClick()
        compose.onNodeWithText("Grant permissions").performScrollTo().performClick()
        compose.onNodeWithText("Exempt from battery optimization").performScrollTo().performClick()

        assertEquals(listOf("role", "permissions", "battery"), calls)
    }

    @Test
    fun `continue stays disabled until role and permissions are granted`() {
        show(PermissionsState(hasRole = true))

        compose.onNodeWithText("Continue").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `granted steps drop their button and optional battery does not block continue`() {
        show(PermissionsState(hasRole = true, hasPermissions = true))

        compose.onNodeWithText("Set as call-screening app").assertDoesNotExist()
        compose.onNodeWithText("Grant permissions").assertDoesNotExist()
        compose.onNodeWithText("Optional").assertExists()
        compose
            .onNodeWithText("Continue")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf("continue"), calls)
    }

    @Test
    fun `skip is always available`() {
        show(PermissionsState())

        compose.onNodeWithText("Skip for now").performScrollTo().performClick()

        assertEquals(listOf("skip"), calls)
    }
}
