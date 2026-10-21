package com.sforca.callmanager.feature.onboarding

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences
import com.sforca.callmanager.testing.ComposeHostRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingRulesScreenTest {
    @get:Rule(order = 0)
    val host = ComposeHostRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(preferences: UserPreferences = UserPreferences()) =
        compose.setContent {
            OnboardingRulesScreen(
                preferences = preferences,
                onBlockUnknownChange = { calls += "unknown=$it" },
                onActionChange = { calls += "action=$it" },
                onFinish = { calls += "finish" },
            )
        }

    @Test
    fun `toggling unknown callers reports the new value`() {
        show()

        compose.onNode(isToggleable()).performScrollTo().performClick()

        assertEquals(listOf("unknown=true"), calls)
    }

    @Test
    fun `choosing reject reports the action`() {
        show()

        compose.onNodeWithText("Reject").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf("action=${BlockedCallAction.REJECT}"), calls)
    }

    @Test
    fun `hint follows the selected action`() {
        show(UserPreferences(blockedCallAction = BlockedCallAction.REJECT))

        compose.onNodeWithText("Rejected outright. It never appears as an incoming call.").assertExists()
    }

    @Test
    fun `finish leaves the page`() {
        show()

        compose.onNodeWithText("Finish setup").performScrollTo().performClick()

        assertEquals(listOf("finish"), calls)
    }
}
