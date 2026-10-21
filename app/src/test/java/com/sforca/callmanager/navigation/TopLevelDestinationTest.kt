package com.sforca.callmanager.navigation

import android.content.Context
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.feature.rules.RulesStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TopLevelDestinationTest {
    private fun controller(start: Route = Route.Home): NavHostController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph =
                createGraph(startDestination = start) {
                    composable<Route.Onboarding> {}
                    composable<Route.Home> {}
                    composable<Route.Recent> {}
                    composable<Route.CallDetail> {}
                    composable<Route.Lists> {}
                    composable<Route.Rules> {}
                    composable<Route.RulesStep> {}
                }
        }
    }

    @Test
    fun `the bottom bar has four tabs`() {
        assertEquals(
            listOf(
                TopLevelDestination.HOME,
                TopLevelDestination.RECENT,
                TopLevelDestination.LISTS,
                TopLevelDestination.RULES,
            ),
            TopLevelDestination.entries.toList(),
        )
    }

    @Test
    fun `from resolves the current tab and ignores non top-level destinations`() {
        val nav = controller()
        assertEquals(TopLevelDestination.HOME, TopLevelDestination.from(nav.currentDestination))

        nav.navigate(Route.CallDetail("+15550000000"))
        assertNull(TopLevelDestination.from(nav.currentDestination))
        assertFalse(TopLevelDestination.HOME.matches(nav.currentDestination))
        assertFalse(TopLevelDestination.HOME.matches(null))
    }

    @Test
    fun `rules step pages belong to the Rules tab`() {
        val nav = controller()
        nav.navigate(Route.RulesStep(RulesStep.LISTS))

        assertEquals(TopLevelDestination.RULES, TopLevelDestination.from(nav.currentDestination))
        assertFalse(TopLevelDestination.LISTS.matches(nav.currentDestination))
    }

    @Test
    fun `call detail route carries the number`() {
        val nav = controller()
        nav.navigate(Route.CallDetail("+15550000000"))

        assertEquals("+15550000000", nav.currentBackStackEntry!!.toRoute<Route.CallDetail>().number)
    }

    @Test
    fun `switching tabs keeps a single copy of each and back leaves the app from the start tab`() {
        val nav = controller()
        nav.navigateToTopLevel(TopLevelDestination.LISTS)
        nav.navigateToTopLevel(TopLevelDestination.RULES)
        nav.navigateToTopLevel(TopLevelDestination.LISTS)

        assertTrue(TopLevelDestination.LISTS.matches(nav.currentDestination))
        nav.popBackStack()
        assertTrue(TopLevelDestination.HOME.matches(nav.currentDestination))
    }

    @Test
    fun `a tab's own back stack is saved and restored when switching away and back`() {
        val nav = controller()
        nav.navigateToTopLevel(TopLevelDestination.RECENT)
        nav.navigate(Route.CallDetail("+15550000000"))
        nav.navigateToTopLevel(TopLevelDestination.RULES)
        nav.navigateToTopLevel(TopLevelDestination.RECENT)

        assertEquals("+15550000000", nav.currentBackStackEntry!!.toRoute<Route.CallDetail>().number)
    }

    @Test
    fun `leaving onboarding removes it from the back stack`() {
        val nav = controller(start = Route.Onboarding)
        nav.navigate(Route.Home) { popUpTo<Route.Onboarding> { inclusive = true } }

        assertTrue(TopLevelDestination.HOME.matches(nav.currentDestination))
        assertFalse(nav.popBackStack())
    }
}
