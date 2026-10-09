package com.matheus.callmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.matheus.callmanager.BuildConfig
import com.matheus.callmanager.feature.debug.DebugScreen
import com.matheus.callmanager.feature.lists.ListsScreen
import com.matheus.callmanager.feature.onboarding.OnboardingScreen
import com.matheus.callmanager.feature.onboarding.PermissionsState
import com.matheus.callmanager.feature.recent.CallDetailRoute
import com.matheus.callmanager.feature.recent.RecentScreen
import com.matheus.callmanager.feature.rules.RulesScreen

@Composable
fun CallManagerNavHost(
    navController: NavHostController,
    startDestination: Route,
    permissions: PermissionsState,
    actions: SystemActions,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<Route.Onboarding> {
            val finish = { navController.leaveOnboarding() }
            OnboardingScreen(
                state = permissions,
                onRequestRole = actions.requestScreeningRole,
                onRequestPermissions = actions.requestRuntimePermissions,
                onRequestBatteryExemption = actions.requestBatteryExemption,
                onSkip = {
                    actions.skipOnboarding()
                    finish()
                },
                onFinished = finish,
            )
        }
        composable<Route.Recent>(deepLinks = listOf(navDeepLink<Route.Recent>(basePath = "${DEEP_LINK_BASE}recent"))) {
            RecentScreen(onCallSelected = { navController.navigate(Route.CallDetail(it)) })
        }
        composable<Route.CallDetail>(
            deepLinks = listOf(navDeepLink<Route.CallDetail>(basePath = "${DEEP_LINK_BASE}call")),
        ) { entry ->
            CallDetailRoute(
                number = entry.toRoute<Route.CallDetail>().number,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Lists>(deepLinks = listOf(navDeepLink<Route.Lists>(basePath = "${DEEP_LINK_BASE}lists"))) {
            ListsScreen(
                onPickContactToBlock = actions.pickContactToBlock,
                onPickContactToAllow = actions.pickContactToAllow,
            )
        }
        composable<Route.Rules>(deepLinks = listOf(navDeepLink<Route.Rules>(basePath = "${DEEP_LINK_BASE}rules"))) {
            RulesScreen(
                isIgnoringBatteryOptimizations = permissions.batteryExempt,
                onRequestIgnoreBatteryOptimizations = actions.requestBatteryExemption,
            )
        }
        if (BuildConfig.DEBUG) {
            composable<Route.Debug> { DebugScreen() }
        }
    }
}

/** Switches tabs while saving/restoring each tab's back stack and state. */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) { configureTopLevel() }
}

private fun NavOptionsBuilder.configureTopLevel() {
    popUpTo<Route.Recent> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavHostController.leaveOnboarding() {
    navigate(Route.Recent) { popUpTo<Route.Onboarding> { inclusive = true } }
}
