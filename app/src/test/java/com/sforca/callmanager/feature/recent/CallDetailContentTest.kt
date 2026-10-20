package com.sforca.callmanager.feature.recent

import android.provider.CallLog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.RecentCallGroup
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.testing.ComposeHostActivityRule
import com.sforca.callmanager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NUMBER = "+15550000001"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallDetailContentTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private fun group(
        types: List<Int>,
        name: String? = "Alice",
    ) = RecentCallGroup(
        number = NUMBER,
        name = name,
        calls = types.mapIndexed { i, type -> RecentCall(NUMBER, name, 1_000L * (types.size - i), type) },
    )

    private fun show(
        group: RecentCallGroup,
        status: NumberStatus = NumberStatus.NONE,
        blockedCalls: List<UnifiedBlockedCall> = emptyList(),
        onBack: () -> Unit = {},
        onToggleAllow: () -> Unit = {},
        onToggleBlock: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme {
                CallDetailContent(
                    group = group,
                    status = status,
                    blockedCalls = blockedCalls,
                    defaultRegion = "US",
                    onBack = onBack,
                    onToggleAllow = onToggleAllow,
                    onToggleBlock = onToggleBlock,
                )
            }
        }
    }

    @Test
    fun `shows the contact name and a status pill for a blocked number`() {
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE)), status = NumberStatus.BLOCKED)

        compose.onNodeWithText("Alice").assertExists()
        compose.onNodeWithText("Blocked").assertExists()
    }

    @Test
    fun `shows no status pill for a number on neither list`() {
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE)))

        compose.onNodeWithText("Blocked").assertDoesNotExist()
        compose.onNodeWithText("Allowed").assertDoesNotExist()
    }

    @Test
    fun `back button calls onBack`() {
        var back = 0
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE)), onBack = { back++ })

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    @Test
    fun `block button toggles the block list and reads Unblock when blocked`() {
        var toggled = 0
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE)), onToggleBlock = { toggled++ })
        compose.onNodeWithText("Block").performClick()
        assertEquals(1, toggled)
    }

    @Test
    fun `a blocked number offers Unblock`() {
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE)), status = NumberStatus.BLOCKED)

        compose.onNodeWithText("Unblock").assertExists()
    }

    @Test
    fun `allow button toggles the allow list and reads Remove when allowed`() {
        var toggled = 0
        show(
            group(types = listOf(CallLog.Calls.MISSED_TYPE)),
            status = NumberStatus.ALLOWED,
            onToggleAllow = { toggled++ },
        )

        compose.onNodeWithText("Remove from allow list").performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun `lists the call history with each outcome`() {
        show(group(types = listOf(CallLog.Calls.MISSED_TYPE, CallLog.Calls.INCOMING_TYPE)))

        compose.onNodeWithText("Call history").assertExists()
        compose.onNodeWithText("Missed").assertExists()
        compose.onNodeWithText("Incoming").assertExists()
    }

    @Test
    fun `a call our own log blocked is labelled Blocked in the history`() {
        show(
            group(types = listOf(CallLog.Calls.MISSED_TYPE)),
            blockedCalls = listOf(UnifiedBlockedCall(NUMBER, null, 1_000L, "rule", "app")),
        )

        compose.onNodeWithText("Blocked").assertExists()
    }

    @Test
    fun `shows an empty state when there is no history for the number`() {
        show(RecentCallGroup(NUMBER, null, emptyList()))

        compose.onNodeWithText("No calls found").assertExists()
    }
}
