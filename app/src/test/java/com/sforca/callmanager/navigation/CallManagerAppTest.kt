package com.sforca.callmanager.navigation

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.provider.CallLog
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.MainActivity
import com.sforca.callmanager.core.datastore.SettingsRepository
import com.sforca.callmanager.core.di.PermissionsModule
import com.sforca.callmanager.core.di.TelephonyModule
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.SystemBlockedCall
import com.sforca.callmanager.core.model.ThemeMode
import com.sforca.callmanager.core.telephony.CallLogDataSource
import com.sforca.callmanager.core.telephony.ContactsLookup
import com.sforca.callmanager.core.telephony.RecentOutgoingLookup
import com.sforca.callmanager.feature.onboarding.FakeSystemPermissionStatus
import com.sforca.callmanager.feature.onboarding.SystemPermissionStatus
import com.sforca.callmanager.testing.FakeContactsLookup
import com.sforca.callmanager.testing.FakeRecentOutgoingLookup
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

private const val NUMBER = "+15550000000"

private class StaticCallLog(
    private val calls: List<RecentCall>,
) : CallLogDataSource {
    override fun recentCalls(offset: Int): List<RecentCall> = if (offset == 0) calls else emptyList()

    override fun systemBlockedCalls(offset: Int): List<SystemBlockedCall> = emptyList()
}

/** Drives the real nav graph, shell and onboarding through `MainActivity` with Hilt and fake system seams. */
@HiltAndroidTest
@UninstallModules(PermissionsModule::class, TelephonyModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34])
class CallManagerAppTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject
    lateinit var settings: SettingsRepository

    private val fakeStatus = FakeSystemPermissionStatus()

    @BindValue
    @JvmField
    val status: SystemPermissionStatus = fakeStatus

    @BindValue
    @JvmField
    val contacts: ContactsLookup = FakeContactsLookup()

    @BindValue
    @JvmField
    val outgoing: RecentOutgoingLookup = FakeRecentOutgoingLookup()

    @BindValue
    @JvmField
    val callLog: CallLogDataSource =
        StaticCallLog(
            listOf(RecentCall(number = NUMBER, name = "Alice", timestamp = 1_000L, type = CallLog.Calls.MISSED_TYPE)),
        )

    private var scenario: ActivityScenario<MainActivity>? = null

    private var injected = false

    private fun inject() {
        if (!injected) hilt.inject()
        injected = true
    }

    private fun launch(deepLink: String? = null) {
        inject()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val intent =
            if (deepLink == null) {
                Intent(context, MainActivity::class.java)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
            }
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        scenario?.close()
        // Robolectric's main thread outlives the test. Finish the activity's teardown work now so it
        // can't run during the next test and race that test's Dispatchers.setMain.
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun grantEverything() {
        fakeStatus.role = true
        fakeStatus.permissions = true
        fakeStatus.battery = true
    }

    @Test
    fun `tabs switch between top-level screens`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Protection is on").assertExists()
        compose.onNodeWithText("Recent").performClick()
        compose.onNodeWithText("Search name or number").assertExists()
        compose.onNodeWithText("Lists").performClick()
        compose.onNodeWithText("No blocked numbers").assertExists()
        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("Guided setup").assertExists()
        compose.onNodeWithText("Test").performClick()
        compose.onNodeWithText("Test number").assertExists()
    }

    @Test
    fun `rules hub test a number button opens the Test a number tab`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("Test a number").performClick()

        compose.onNodeWithText("Test number").assertExists()
    }

    @Test
    fun `test deep link opens Test a number`() {
        grantEverything()
        launch(deepLink = "callmanager://test")

        compose.onNodeWithText("Test number").assertExists()
    }

    @Test
    fun `gear icon on Home opens settings and back returns to Home`() {
        grantEverything()
        launch()

        compose.onNodeWithContentDescription("Open settings").performClick()
        compose.onNodeWithText("Appearance").assertExists()
        compose.onNodeWithText("Version", substring = true).performScrollTo().assertExists()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Protection is on").assertExists()
    }

    @Test
    fun `settings deep link opens settings without a navigation bar`() {
        grantEverything()
        launch(deepLink = "callmanager://settings")

        compose.onNodeWithText("Appearance").assertExists()
        compose.onNodeWithText("Rules").assertDoesNotExist()
    }

    @Test
    fun `settings theme choice is saved and permissions shortcut opens app details`() {
        grantEverything()
        launch(deepLink = "callmanager://settings")

        compose.onNodeWithText("Dark").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(timeoutMillis = 5_000) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
            kotlinx.coroutines.runBlocking { settings.preferences.first().themeMode } == ThemeMode.DARK
        }
        compose.onNodeWithText("Permissions").performScrollTo().performClick()

        val started = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
    }

    @Test
    fun `rules hub opens a step and back returns to the hub on the Rules tab`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("People you know").performClick()
        compose.onNodeWithText("2. People you know").assertExists()
        compose.onNodeWithText("Guided setup").assertDoesNotExist()

        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Guided setup").assertExists()
    }

    @Test
    fun `guided setup walks every step in order and ends on the hub`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("Guided setup").performClick()
        listOf(
            "1. Your lists",
            "2. People you know",
            "3. Unknown callers",
            "4. When a call is blocked",
        ).forEachIndexed {
                i,
                header,
            ->
            compose.onNodeWithText(header).assertExists()
            compose.onNodeWithText(if (i == 3) "Done" else "Next").performScrollTo().performClick()
        }

        compose.onNodeWithText("Guided setup").assertExists()
    }

    @Test
    fun `lists step opens the Lists tab`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("Your lists").performClick()
        compose.onNodeWithText("Open Lists").performScrollTo().performClick()

        compose.onNodeWithText("No blocked numbers").assertExists()
    }

    @Test
    fun `selecting a call opens its detail destination and back returns to the list`() {
        grantEverything()
        launch()

        compose.onNodeWithText("Recent").performClick()
        compose.onNodeWithText("Alice").performClick()
        compose.onNodeWithText("Call history").assertExists()
        compose.onNodeWithText("Search name or number").assertDoesNotExist()

        compose.onNodeWithText("Alice").assertExists()
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Search name or number").assertExists()
    }

    @Test
    fun `call detail can be opened directly from a deep link`() {
        grantEverything()
        launch(deepLink = "callmanager://call/%2B15550000000")

        compose.onNodeWithText("Call history").assertExists()
    }

    @Test
    fun `first run shows every permission on one page and skipping reveals the app with a fix banner`() {
        launch()

        compose.onNodeWithText("Set as call-screening app").assertExists()
        compose.onNodeWithText("Grant permissions").assertExists()
        compose.onNodeWithText("Exempt from battery optimization").assertExists()
        compose.onNodeWithText("Skip for now").performScrollTo().performClick()

        // Skipping permissions still offers the key rules once.
        awaitText("Finish setup")
        compose.onNodeWithText("Finish setup").performScrollTo().performClick()
        awaitText("Needs attention")

        compose
            .onNodeWithText("Call Manager isn't your call-screening app, so calls aren't being screened.")
            .assertExists()
        compose.onNodeWithText("Needs attention").assertExists()
    }

    @Test
    fun `granting permissions leads to the rules page and then Home with a Rules hint`() {
        launch()
        compose.onNodeWithText("Continue").assertIsNotEnabled()

        fakeStatus.role = true
        fakeStatus.permissions = true
        resumeActivity()
        compose
            .onNodeWithText("Continue")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        compose.onNodeWithText("Choose your key rules").assertExists()
        compose.onNodeWithText("Reject").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Finish setup").performScrollTo().performClick()

        awaitText("Protection is on")
        compose.onNodeWithText("Protection is on. You can add more customizable rules in the Rules tab.").assertExists()
        compose.onNodeWithText("Open Rules").performClick()
        compose.onNodeWithText("Guided setup").assertExists()

        val prefs = kotlinx.coroutines.runBlocking { settings.preferences.first() }
        assertEquals(BlockedCallAction.REJECT, prefs.blockedCallAction)
        assertEquals(true, prefs.blockUnknownNumbers)
        assertEquals(true, prefs.rulesSetupCompleted)
    }

    @Test
    fun `the rules page is only shown the first time`() {
        inject()
        kotlinx.coroutines.runBlocking { settings.setRulesSetupCompleted(true) }
        launch()
        fakeStatus.role = true
        fakeStatus.permissions = true
        resumeActivity()

        compose.onNodeWithText("Continue").performScrollTo().performClick()

        awaitText("Protection is on")
        compose.onNodeWithText("Choose your key rules").assertDoesNotExist()
    }

    @Test
    fun `already-screening users with core steps done skip onboarding entirely`() {
        fakeStatus.role = true
        fakeStatus.permissions = true
        launch()

        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.onNodeWithText("Protection is on").assertExists()
    }

    @Test
    fun `home is the start tab and See all opens Recent`() {
        grantEverything()
        launch()

        compose.onNodeWithText("See all").performClick()

        compose.onNodeWithText("Search name or number").assertExists()
    }

    /** Waits for a transition that hops through DataStore's IO thread, which compose idling can't see. */
    private fun awaitText(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun resumeActivity() {
        scenario!!.moveToState(Lifecycle.State.STARTED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
    }
}
