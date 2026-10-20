package com.matheus.callmanager.feature.rules

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.testing.ComposeHostActivityRule
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.MainDispatcherRule
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RulesScreenTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private lateinit var db: CallManagerDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: RulesViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        viewModel = RulesViewModel(CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), settings))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun show(
        batteryExempt: Boolean = true,
        dynamicColorSupported: Boolean = true,
        onBattery: () -> Unit = {},
        state: RulesUiState = RulesUiState(),
    ) {
        compose.setContent {
            CallManagerTheme {
                RulesContent(state, batteryExempt, onBattery, dynamicColorSupported, viewModel)
            }
        }
    }

    private fun prefs() = runBlocking { settings.preferences.first() }

    @Test
    fun `battery card appears only when not exempt and fires the request`() {
        var requested = false
        show(batteryExempt = false, onBattery = { requested = true })

        compose.onNodeWithText("Exempt from battery optimization").performClick()

        assertTrue(requested)
    }

    @Test
    fun `battery card is hidden when exempt`() {
        show(batteryExempt = true)

        compose.onNodeWithText("Exempt from battery optimization").assertDoesNotExist()
    }

    @Test
    fun `sliders show live day values and hide when their rule is off`() {
        show(state = RulesUiState(prefs().copy(allowRecentAcceptedDays = 1, recentOutgoingRuleEnabled = false)))

        compose.onNodeWithText("1 day").assertIsDisplayed()
        compose.onNodeWithText("Allow for").assertDoesNotExist()
    }

    @Test
    fun `toggling switches and the action control writes through`() {
        show()

        compose
            .onAllNodes(isToggleable())
            .onFirst()
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Reject").performSemanticsAction(SemanticsActions.OnClick)
        compose
            .onAllNodes(isToggleable())
            .onLast()
            .performScrollTo()
            .performClick()
        compose.waitForIdle()

        assertFalse(prefs().allowContacts)
        assertEquals(BlockedCallAction.REJECT, prefs().blockedCallAction)
        assertTrue(prefs().dynamicColor)
    }

    @Test
    fun `dynamic color toggle is hidden on unsupported devices`() {
        show(dynamicColorSupported = false)

        compose.onNodeWithText("Dynamic color").assertDoesNotExist()
    }

    @Test
    fun `info buttons open the sheet for each rule and it can be dismissed`() {
        show()
        val rules =
            listOf(
                Triple("Contacts", "Allows anyone saved in your contacts.", 4),
                Triple("Recently answered", "Allows a number again for a while after you answered a call from it.", 5),
                Triple(
                    "Recently called",
                    "Allows numbers you called yourself for a while, so a callback is not blocked.",
                    6,
                ),
                Triple(
                    "Unknown numbers",
                    "The last rule. A number that matched nothing above is allowed, or blocked if you turn this on.",
                    7,
                ),
            )

        rules.forEach { (title, description, order) ->
            compose.onNodeWithContentDescription("About $title").performScrollTo().performClick()
            compose.onNodeWithText(description).assertIsDisplayed()
            compose.onNodeWithText("Rule $order").assertIsDisplayed()
            compose.onNodeWithText("Example").assertIsDisplayed()
            compose
                .onNode(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss),
                ).performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(description).fetchSemanticsNodes().isEmpty()
            }
        }
    }
}
