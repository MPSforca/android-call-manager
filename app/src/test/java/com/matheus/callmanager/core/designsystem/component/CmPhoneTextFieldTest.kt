package com.matheus.callmanager.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.testing.ComposeHostActivityRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CmPhoneTextFieldTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(compose)

    @Test
    fun `formats digits as the user types`() {
        var text by mutableStateOf("")
        compose.setContent {
            CallManagerTheme {
                CmPhoneTextField(text, { text = it }, "Phone", defaultRegion = "US")
            }
        }

        compose.onNodeWithText("Phone").performTextInput("4155550100")

        assertEquals("(415) 555-0100", text)
    }
}
