package com.matheus.callmanager.navigation

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.provider.CallLog
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.BuildConfig
import com.matheus.callmanager.MainActivity
import com.matheus.callmanager.core.datastore.SettingsRepository
import com.matheus.callmanager.core.di.PermissionsModule
import com.matheus.callmanager.core.di.TelephonyModule
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.core.model.ThemeMode
import com.matheus.callmanager.core.telephony.CallLogDataSource
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import com.matheus.callmanager.feature.onboarding.FakeSystemPermissionStatus
import com.matheus.callmanager.feature.onboarding.SystemPermissionStatus
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
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

    private fun launch(deepLink: String? = null) {
        hilt.inject()
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

        compose.onNodeWithText("Search name or number").assertExists()
        compose.onNodeWithText("Lists").performClick()
        compose.onNodeWithText("No blocked numbers").assertExists()
        compose.onNodeWithText("Rules").performClick()
        compose.onNodeWithText("Guided setup").assertExists()
        if (BuildConfig.DEBUG) {
            compose.onNodeWithText("Debug").performClick()
            compose.onNodeWithText("Test number").assertExists()
        } else {
            compose.onNodeWithText("Debug").assertDoesNotExist()
        }
    }

    @Test
    fun `gear icon opens settings and back returns to Recent`() {
        grantEverything()
        launch()

        compose.onNodeWithContentDescription("Open settings").performClick()
        compose.onNodeWithText("Appearance").assertExists()
        compose.onNodeWithText("Version", substring = true).performScrollTo().assertExists()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Search name or number").assertExists()
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
    fun `first run shows onboarding and skipping reveals the app with a fix banner`() {
        launch()

        compose.onNodeWithText("Step 1 of 3").assertExists()
        compose.onNodeWithText("Skip for now").performClick()

        compose
            .onNodeWithText("Call Manager isn't your call-screening app, so calls aren't being screened.")
            .assertExists()
        compose.onNodeWithText("Fix").performClick()
        compose.onNodeWithText("Recent").assertExists()
    }

    @Test
    fun `onboarding moves on once the system reports the role was granted`() {
        launch()
        compose.onNodeWithText("Set as call-screening app").performClick()

        fakeStatus.role = true
        resumeActivity()

        compose.onNodeWithText("Step 2 of 3").assertExists()
        compose.onNodeWithText("Grant permissions").performClick()
    }

    @Test
    fun `declining the optional battery step enters the app`() {
        launch()
        fakeStatus.role = true
        fakeStatus.permissions = true
        resumeActivity()

        compose.onNodeWithText("Step 3 of 3").assertExists()
        compose.onNodeWithText("Exempt from battery optimization").performClick()
        compose.onNodeWithText("Not now").performClick()

        compose.onNodeWithText("Search name or number").assertExists()
    }

    @Test
    fun `already-screening users with core steps done skip onboarding entirely`() {
        fakeStatus.role = true
        fakeStatus.permissions = true
        launch()

        compose.onNodeWithText("Step 3 of 3").assertDoesNotExist()
        compose.onNodeWithText("Search name or number").assertExists()
    }

    private fun resumeActivity() {
        scenario!!.moveToState(Lifecycle.State.STARTED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
    }
}
