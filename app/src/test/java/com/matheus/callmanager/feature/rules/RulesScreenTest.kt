package com.matheus.callmanager.feature.rules

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.data.rule.ScreeningTrace
import com.matheus.callmanager.core.data.rule.TraceOutcome
import com.matheus.callmanager.core.data.rule.TraceStep
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.model.UserPreferences
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

    private fun showHub(
        batteryExempt: Boolean = true,
        dynamicColorSupported: Boolean = true,
        onBattery: () -> Unit = {},
        onOpenStep: (RulesStep) -> Unit = {},
        onGuided: () -> Unit = {},
        onPreview: (String, String?) -> Unit = { _, _ -> },
        state: RulesUiState = RulesUiState(),
    ) {
        compose.setContent {
            CallManagerTheme {
                RulesHubContent(
                    state,
                    batteryExempt,
                    onBattery,
                    dynamicColorSupported,
                    RulesHubActions(onOpenStep, onGuided, { viewModel.setDynamicColor(it) }, onPreview),
                )
            }
        }
    }

    private fun showStep(
        step: RulesStep,
        guided: Boolean = false,
        state: RulesUiState = RulesUiState(),
        onOpenLists: () -> Unit = {},
        onAdvance: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme {
                RulesStepContent(step, state, viewModel, guided, onOpenLists, onAdvance)
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

    private val allowTrace =
        ScreeningTrace(
            ScreeningDecision.Allow("Contact"),
            listOf(
                TraceStep(ScreeningRuleId.BLOCK_LIST, TraceOutcome.PASSED),
                TraceStep(ScreeningRuleId.CONTACT, TraceOutcome.DECIDED),
            ),
        )

    @Test
    fun `preview card reports typed numbers and names, normalised`() {
        val calls = mutableListOf<Pair<String, String?>>()
        showHub(onPreview = { n, name -> calls += n to name })

        compose.onNodeWithText("Phone number").performScrollTo().performTextInput("+14155552671")
        compose.onNodeWithText("Caller name (optional)").performScrollTo().performTextInput("Dr. Who")

        assertEquals("+14155552671", calls.last().first)
        assertEquals("Dr. Who", calls.last().second)
    }

    @Test
    fun `preview card shows the verdict and which rule decided it`() {
        showHub(state = RulesUiState(preview = RulePreview("+15550000001", allowTrace)), onPreview = { _, _ -> })
        compose.onNodeWithText("Phone number").performScrollTo().performTextInput("+15550000001")

        compose.onNodeWithText("Would ring").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Decided by: Contacts").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `preview card shows a blocked verdict`() {
        val trace =
            ScreeningTrace(
                ScreeningDecision.Reject("Block list"),
                listOf(TraceStep(ScreeningRuleId.BLOCK_LIST, TraceOutcome.DECIDED)),
            )
        showHub(state = RulesUiState(preview = RulePreview("+15550000001", trace)))
        compose.onNodeWithText("Phone number").performScrollTo().performTextInput("+15550000001")

        compose.onNodeWithText("Would be blocked").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Decided by: Block list").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `preview card hides the verdict when the field is empty`() {
        showHub(state = RulesUiState(preview = RulePreview("+15550000001", allowTrace)))

        compose.onNodeWithText("Would ring").assertDoesNotExist()
    }

    @Test
    fun `step pages show the preview and send it to the view model`() {
        showStep(RulesStep.UNKNOWN_CALLERS)

        compose.onNodeWithText("Phone number").performScrollTo().performTextInput("+15550000001")
        compose.waitForIdle()

        compose.onNodeWithText("Try a number").assertExists()
    }

    @Test
    fun `hub guided setup button fires`() {
        var guided = false
        showHub(onGuided = { guided = true })

        compose.onNodeWithText("Guided setup").performClick()

        assertTrue(guided)
    }

    @Test
    fun `hub battery card appears only when not exempt and fires the request`() {
        var requested = false
        showHub(batteryExempt = false, onBattery = { requested = true })

        compose.onNodeWithText("Exempt from battery optimization").performScrollTo().performClick()

        assertTrue(requested)
    }

    @Test
    fun `hub hides the battery card when exempt`() {
        showHub(batteryExempt = true)

        compose.onNodeWithText("Exempt from battery optimization").assertDoesNotExist()
    }

    @Test
    fun `hub dynamic color toggle writes through and is hidden on unsupported devices`() {
        showHub()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(prefs().dynamicColor)
    }

    @Test
    fun `hub dynamic color toggle is hidden on unsupported devices`() {
        showHub(dynamicColorSupported = false)

        compose.onNodeWithText("Dynamic color").assertDoesNotExist()
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
    fun `guided steps show Next, and Done on the last step`() {
        var advanced = 0
        showStep(RulesStep.UNKNOWN_CALLERS, guided = true, onAdvance = { advanced++ })
        compose.onNodeWithText("Next").performScrollTo().performClick()
        assertEquals(1, advanced)
    }

    @Test
    fun `guided last step shows Done and unguided steps show no button`() {
        showStep(RulesStep.BLOCKED_CALLS, guided = true)
        compose.onNodeWithText("Done").assertExists()
    }

    @Test
    fun `unguided steps have no Next button`() {
        showStep(RulesStep.UNKNOWN_CALLERS)
        compose.onNodeWithText("Next").assertDoesNotExist()
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
            CallManagerTheme { RulesStepContent(current, RulesUiState(), viewModel, false, {}, {}) }
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
