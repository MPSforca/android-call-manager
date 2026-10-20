package com.sforca.callmanager.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.testing.ComposeHostActivityRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionBannerTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(compose)

    @Test
    fun `shows the message and fires the action`() {
        var clicks = 0
        compose.setContent {
            CallManagerTheme { PermissionBanner("Missing access", "Fix", { clicks++ }) }
        }

        compose.onNodeWithText("Missing access").assertExists()
        compose.onNodeWithText("Fix").performClick()

        assertEquals(1, clicks)
    }
}
