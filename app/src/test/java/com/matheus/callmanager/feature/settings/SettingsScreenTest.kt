package com.matheus.callmanager.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.model.ThemeMode
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.testing.ComposeHostActivityRule
import com.matheus.callmanager.testing.MainDispatcherRule
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
class SettingsScreenTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private var theme: ThemeMode? = null
    private var dynamic: Boolean? = null
    private var batteryRequested = false
    private var permissionsOpened = false
    private val urls = mutableListOf<String>()

    private fun show(
        preferences: UserPreferences = UserPreferences(),
        batteryExempt: Boolean = false,
        dynamicColorSupported: Boolean = true,
    ) {
        compose.setContent {
            CallManagerTheme {
                SettingsContent(
                    preferences,
                    batteryExempt,
                    dynamicColorSupported,
                    versionName = "1.2.3",
                    actions =
                        SettingsActions(
                            onThemeModeChange = { theme = it },
                            onDynamicColorChange = { dynamic = it },
                            onRequestIgnoreBatteryOptimizations = { batteryRequested = true },
                            onOpenUrl = { urls += it },
                            onOpenPermissions = { permissionsOpened = true },
                        ),
                )
            }
        }
    }

    @Test
    fun `theme control reports the chosen mode`() {
        show()

        compose.onNodeWithText("Dark").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(ThemeMode.DARK, theme)
        compose.onNodeWithText("Light").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(ThemeMode.LIGHT, theme)
        compose.onNodeWithText("System").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(ThemeMode.SYSTEM, theme)
    }

    @Test
    fun `dynamic color toggle reports the change`() {
        show(preferences = UserPreferences(dynamicColor = false))

        compose.onNode(isToggleable()).performScrollTo().performClick()

        assertEquals(true, dynamic)
    }

    @Test
    fun `dynamic color is hidden below Android 12`() {
        show(dynamicColorSupported = false)

        compose.onNodeWithText("Dynamic color").assertDoesNotExist()
    }

    @Test
    fun `battery card requests the exemption only when not exempt`() {
        show(batteryExempt = false)

        compose.onNodeWithText("Exempt from battery optimization").performScrollTo().performClick()

        assertTrue(batteryRequested)
    }

    @Test
    fun `battery section confirms the exemption when exempt`() {
        show(batteryExempt = true)

        compose.onNodeWithText("Exempt from battery optimization").assertDoesNotExist()
        compose.onNodeWithText("This app is exempt from battery optimization.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `about section shows the version and opens links and permissions`() {
        show()

        compose.onNodeWithText("Version 1.2.3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()
        compose.onNodeWithText("License").performScrollTo().performClick()
        compose.onNodeWithText("Permissions").performScrollTo().performClick()

        assertEquals(listOf(PRIVACY_URL, LICENSE_URL), urls)
        assertTrue(permissionsOpened)
    }
}
