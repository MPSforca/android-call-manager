package com.matheus.callmanager.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.testing.ComposeHostActivityRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CmSliderRowTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(compose)

    @Test
    fun `shows the formatted value and commits once when the drag ends`() {
        val committed = mutableListOf<Int>()
        compose.setContent {
            CallManagerTheme {
                CmSliderRow("Window", 10, { committed += it }, 1..30, { "$it days" })
            }
        }

        compose.onNodeWithText("10 days").assertExists()
        compose.onRoot().performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertEquals(1, committed.size)
        assertTrue(committed.single() > 10)
    }
}
