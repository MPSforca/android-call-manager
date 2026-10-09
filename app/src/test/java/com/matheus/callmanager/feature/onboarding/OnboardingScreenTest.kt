package com.matheus.callmanager.feature.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.matheus.callmanager.testing.ComposeHostRule
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
                onFinished = { calls += "finished" },
            )
        }

    @Test
    fun `role step explains itself and requests the role`() {
        show(PermissionsState())

        compose.onNodeWithText("Step 1 of 3").assertExists()
        compose.onNodeWithText("Set as call-screening app").performClick()
        compose.onNodeWithText("Skip for now").performClick()

        assertEquals(listOf("role", "skip"), calls)
    }

    @Test
    fun `permissions step requests runtime permissions`() {
        show(PermissionsState(hasRole = true))

        compose.onNodeWithText("Step 2 of 3").assertExists()
        compose.onNodeWithText("Grant permissions").performClick()

        assertEquals(listOf("permissions"), calls)
    }

    @Test
    fun `battery step can be declined without skipping onboarding`() {
        show(PermissionsState(hasRole = true, hasPermissions = true))

        compose.onNodeWithText("Exempt from battery optimization").performClick()
        compose.onNodeWithText("Not now").performClick()

        assertEquals(listOf("battery", "finished"), calls)
    }

    @Test
    fun `finishes immediately when everything is already granted`() {
        show(PermissionsState(hasRole = true, hasPermissions = true, batteryExempt = true))
        compose.waitForIdle()

        assertEquals(listOf("finished"), calls)
    }
}
