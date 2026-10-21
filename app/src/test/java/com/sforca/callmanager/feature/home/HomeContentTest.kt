package com.sforca.callmanager.feature.home

import android.provider.CallLog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.feature.onboarding.OnboardingStep
import com.sforca.callmanager.feature.onboarding.PermissionsState
import com.sforca.callmanager.testing.ComposeHostActivityRule
import com.sforca.callmanager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

private val TODAY = LocalDate.of(2026, 10, 18)
private val NOON_TODAY = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
private val ALL_GRANTED = PermissionsState(hasRole = true, hasPermissions = true, batteryExempt = true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeContentTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private fun show(
        state: HomeUiState,
        onFix: (OnboardingStep) -> Unit = {},
        onCallSelected: (String) -> Unit = {},
        onSeeAll: () -> Unit = {},
        onDismissRulesHint: () -> Unit = {},
        onOpenRules: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme {
                HomeContent(
                    state = state,
                    today = TODAY,
                    zone = ZoneOffset.UTC,
                    actions =
                        HomeActions(
                            onFix = onFix,
                            onCallSelected = onCallSelected,
                            onSeeAll = onSeeAll,
                            onDismissRulesHint = onDismissRulesHint,
                            onOpenRules = onOpenRules,
                        ),
                )
            }
        }
    }

    @Test
    fun `reports protection is on when screening can work`() {
        show(HomeUiState(permissions = ALL_GRANTED))

        compose.onNodeWithText("Protection is on").assertExists()
        compose.onNodeWithText("Fix").assertDoesNotExist()
    }

    @Test
    fun `flags the missing role and fixes it`() {
        val fixed = mutableListOf<OnboardingStep>()
        show(
            HomeUiState(permissions = PermissionsState(hasPermissions = true, batteryExempt = true)),
            onFix = { fixed += it },
        )

        compose.onNodeWithText("Needs attention").assertExists()
        compose.onNodeWithText("Fix").performClick()

        assertEquals(listOf(OnboardingStep.ROLE), fixed)
    }

    @Test
    fun `a missing battery exemption is suggested without breaking protection`() {
        show(HomeUiState(permissions = PermissionsState(hasRole = true, hasPermissions = true)))

        compose.onNodeWithText("Protection is on").assertExists()
        compose.onNodeWithText("Fix").assertExists()
    }

    @Test
    fun `shows today's blocked and allowed counts`() {
        show(
            HomeUiState(
                permissions = ALL_GRANTED,
                calls =
                    listOf(
                        RecentCall("+15550000001", "Alice", NOON_TODAY, CallLog.Calls.INCOMING_TYPE),
                        RecentCall("+15550000002", "Bob", NOON_TODAY, CallLog.Calls.MISSED_TYPE),
                    ),
            ),
        )

        compose.onNodeWithText("Blocked today").assertExists()
        compose.onNodeWithText("Allowed today").assertExists()
        compose.onNodeWithText("2").assertExists()
    }

    @Test
    fun `lists recent activity and opens a call`() {
        val selected = mutableListOf<String>()
        show(
            HomeUiState(
                permissions = ALL_GRANTED,
                calls = listOf(RecentCall("+15550000001", "Alice", NOON_TODAY, CallLog.Calls.MISSED_TYPE)),
            ),
            onCallSelected = { selected += it },
        )

        compose.onNodeWithText("Recent activity").assertExists()
        compose.onNodeWithText("Alice").performClick()

        assertEquals(listOf("+15550000001"), selected)
    }

    @Test
    fun `See all opens the full Recent list`() {
        var seeAll = 0
        show(HomeUiState(permissions = ALL_GRANTED), onSeeAll = { seeAll++ })

        compose.onNodeWithText("See all").performClick()

        assertEquals(1, seeAll)
    }

    @Test
    fun `rules hint is hidden unless requested`() {
        show(HomeUiState(permissions = ALL_GRANTED))

        compose.onNodeWithText("Open Rules").assertDoesNotExist()
    }

    @Test
    fun `rules hint points to the Rules tab and can be dismissed`() {
        val events = mutableListOf<String>()
        show(
            HomeUiState(permissions = ALL_GRANTED, showRulesHint = true),
            onDismissRulesHint = { events += "dismiss" },
            onOpenRules = { events += "open" },
        )

        compose.onNodeWithText("Protection is on. You can add more customizable rules in the Rules tab.").assertExists()
        compose.onNodeWithText("Open Rules").performClick()
        compose.onNodeWithText("Dismiss").performClick()

        assertEquals(listOf("open", "dismiss"), events)
    }

    @Test
    fun `an empty feed says there is no activity yet`() {
        show(HomeUiState(permissions = ALL_GRANTED))

        compose.onNodeWithText("No calls yet").assertExists()
    }
}
