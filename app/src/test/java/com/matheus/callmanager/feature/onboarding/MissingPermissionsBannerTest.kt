package com.matheus.callmanager.feature.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.matheus.callmanager.testing.ComposeHostRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MissingPermissionsBannerTest {
    @get:Rule(order = 0)
    val host = ComposeHostRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `missing role shows the role message and fixes the role step`() {
        var fixed: OnboardingStep? = null
        compose.setContent { MissingPermissionsBanner(PermissionsState(), onFix = { fixed = it }) }

        compose
            .onNodeWithText("Call Manager isn't your call-screening app, so calls aren't being screened.")
            .assertExists()
        compose.onNodeWithText("Fix").performClick()

        assertEquals(OnboardingStep.ROLE, fixed)
    }

    @Test
    fun `missing permissions shows the permissions message`() {
        var fixed: OnboardingStep? = null
        compose.setContent {
            MissingPermissionsBanner(PermissionsState(hasRole = true), onFix = { fixed = it })
        }

        compose.onNodeWithText("Call Manager is missing permissions it needs to work.").assertExists()
        compose.onNodeWithText("Fix").performClick()

        assertEquals(OnboardingStep.PERMISSIONS, fixed)
    }

    @Test
    fun `renders nothing when the core steps are done`() {
        compose.setContent {
            MissingPermissionsBanner(PermissionsState(hasRole = true, hasPermissions = true), onFix = {})
        }

        compose.onNodeWithText("Fix").assertDoesNotExist()
    }
}
