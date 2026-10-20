package com.sforca.callmanager.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
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
class CmSegmentedButtonTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(compose)

    @Test
    fun `clicking an option reports it`() {
        var picked: String? = null
        compose.setContent {
            CallManagerTheme {
                CmSegmentedButton(listOf("Silence", "Reject"), "Silence", { picked = it }, { it })
            }
        }

        compose.onNodeWithText("Reject").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals("Reject", picked)
    }
}
