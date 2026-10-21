package com.sforca.callmanager.feature.rules

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.database.CallManagerDatabase
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences
import com.sforca.callmanager.testing.ComposeHostActivityRule
import com.sforca.callmanager.testing.FakeContactsLookup
import com.sforca.callmanager.testing.FakeRecentOutgoingLookup
import com.sforca.callmanager.testing.FakeSettingsRepository
import com.sforca.callmanager.testing.MainDispatcherRule
import com.sforca.callmanager.testing.inMemoryTestDatabase
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

    private fun showHub(
        onOpenStep: (RulesStep) -> Unit = {},
        state: RulesUiState = RulesUiState(),
    ) {
        compose.setContent {
            CallManagerTheme {
                RulesHubContent(state, onOpenStep)
            }
        }
    }

    private fun showStep(
        step: RulesStep,
        state: RulesUiState = RulesUiState(),
        onOpenLists: () -> Unit = {},
        onBackToRules: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme {
                RulesStepContent(step, state, viewModel, onOpenLists, onBackToRules, onPrevious, onNext)
            }
        }
    }

    private fun prefs() = runBlocking { settings.preferences.first() }

    @Test
    fun `hub lists every step with its status and opens it on tap`() {
        var opened: RulesStep? = null
        showHub(
            onOpenStep = { opened = it },
            state = RulesUiState(UserPreferences(blockUnknownNumbers = true), ListCounts(2, 1, 3)),
        )

        compose.onNodeWithText("Blocked: 2 · Allowed: 1 · Patterns: 3").assertIsDisplayed()
        compose.onNodeWithText("Unknown numbers are blocked").assertIsDisplayed()
        compose.onNodeWithText("Unknown callers").performScrollTo().performClick()

        assertEquals(RulesStep.UNKNOWN_CALLERS, opened)
    }

    @Test
    fun `lists step shows counts and opens the Lists tab`() {
        var opened = false
        showStep(
            RulesStep.LISTS,
            state = RulesUiState(listCounts = ListCounts(1, 2, 0)),
            onOpenLists = { opened = true },
        )

        compose.onNodeWithText("1 entry").assertIsDisplayed()
        compose.onNodeWithText("2 entries").assertIsDisplayed()
        compose.onNodeWithText("Open Lists").performScrollTo().performClick()

        assertTrue(opened)
    }

    @Test
    fun `lists step info button explains the rule`() {
        showStep(RulesStep.LISTS)

        compose.onNodeWithContentDescription("About Block list").performScrollTo().performClick()

        compose.onNodeWithText("Numbers you chose to block. This rule runs first and always wins.").assertIsDisplayed()
    }

    @Test
    fun `sliders show live day values and hide when their rule is off`() {
        showStep(
            RulesStep.PEOPLE_YOU_KNOW,
            state = RulesUiState(prefs().copy(allowRecentAcceptedDays = 1, recentOutgoingRuleEnabled = false)),
        )

        compose.onNodeWithText("1 day").assertIsDisplayed()
        compose.onNodeWithText("Allow for").assertDoesNotExist()
    }

    @Test
    fun `step switches and the action control write through`() {
        showStep(RulesStep.PEOPLE_YOU_KNOW)
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.waitForIdle()
        assertFalse(prefs().allowContacts)
    }

    @Test
    fun `blocked calls step writes the action and notification setting`() {
        showStep(RulesStep.BLOCKED_CALLS)

        compose.onNodeWithText("Reject").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(BlockedCallAction.REJECT, prefs().blockedCallAction)
    }

    @Test
    fun `unknown callers step writes through`() {
        showStep(RulesStep.UNKNOWN_CALLERS)

        compose.onNode(isToggleable()).performClick()
        compose.waitForIdle()

        assertTrue(prefs().blockUnknownNumbers)
    }

    @Test
    fun `step pages have a back to rules button`() {
        var back = false
        showStep(RulesStep.UNKNOWN_CALLERS, onBackToRules = { back = true })
        compose.onNodeWithText("Back to Rules").performClick()
        assertTrue(back)
    }

    @Test
    fun `middle steps have working Previous and Next buttons`() {
        var previous = 0
        var next = 0
        showStep(RulesStep.UNKNOWN_CALLERS, onPrevious = { previous++ }, onNext = { next++ })
        compose.onNodeWithText("Previous").performScrollTo().performClick()
        compose.onNodeWithText("Next").performScrollTo().performClick()
        assertEquals(1, previous)
        assertEquals(1, next)
    }

    @Test
    fun `first step disables Previous`() {
        showStep(RulesStep.LISTS)
        compose.onNodeWithText("Previous").assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun `last step disables Next`() {
        showStep(RulesStep.BLOCKED_CALLS)
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithText("Previous").assertIsEnabled()
    }

    @Test
    fun `step pages no longer show the Try a number card`() {
        showStep(RulesStep.UNKNOWN_CALLERS)
        compose.onNodeWithText("Try a number").assertDoesNotExist()
    }

    @Test
    fun `info buttons open the sheet for each rule and it can be dismissed`() {
        val rules =
            listOf(
                Triple(RulesStep.PEOPLE_YOU_KNOW, "Contacts", "Allows anyone saved in your contacts." to 4),
                Triple(
                    RulesStep.PEOPLE_YOU_KNOW,
                    "Recently answered",
                    "Allows a number again for a while after you answered a call from it." to 5,
                ),
                Triple(
                    RulesStep.PEOPLE_YOU_KNOW,
                    "Recently called",
                    "Allows numbers you called yourself for a while, so a callback is not blocked." to 6,
                ),
                Triple(
                    RulesStep.UNKNOWN_CALLERS,
                    "Unknown numbers",
                    "The last rule. A number that matched nothing above is allowed, or blocked if you turn this on." to
                        7,
                ),
            )
        var current: RulesStep by mutableStateOf(RulesStep.PEOPLE_YOU_KNOW)
        compose.setContent {
            CallManagerTheme { RulesStepContent(current, RulesUiState(), viewModel, {}, {}, {}, {}) }
        }

        rules.forEach { (step, title, expected) ->
            val (description, order) = expected
            current = step
            compose.onNodeWithContentDescription("About $title").performScrollTo().performClick()
            compose.onNodeWithText(description).assertIsDisplayed()
            compose.onNodeWithText("Rule $order").assertIsDisplayed()
            compose.onNodeWithText("Example").assertIsDisplayed()
            compose
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
                .performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(description).fetchSemanticsNodes().isEmpty()
            }
        }
    }
}
