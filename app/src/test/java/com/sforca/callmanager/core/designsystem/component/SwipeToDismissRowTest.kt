package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
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
class SwipeToDismissRowTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(compose)

    @Test
    fun `swiping toward the end dismisses`() {
        var dismissed = 0
        compose.setContent {
            CallManagerTheme {
                SwipeToDismissRow(onDismiss = { dismissed++ }) { Text("Row", Modifier.fillMaxWidth().height(48.dp)) }
            }
        }

        compose.onNodeWithText("Row").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertEquals(1, dismissed)
    }

    @Test
    fun `swiping toward the start does nothing`() {
        var dismissed = 0
        compose.setContent {
            CallManagerTheme {
                SwipeToDismissRow(onDismiss = { dismissed++ }) { Text("Row", Modifier.fillMaxWidth().height(48.dp)) }
            }
        }

        compose.onNodeWithText("Row").performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertEquals(0, dismissed)
    }
}
