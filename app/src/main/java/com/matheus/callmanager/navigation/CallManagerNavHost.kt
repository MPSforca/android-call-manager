package com.matheus.callmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
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
import com.matheus.callmanager.feature.rules.RulesStep
import com.matheus.callmanager.feature.rules.RulesStepScreen
import com.matheus.callmanager.feature.settings.SettingsScreen

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
                onOpenStep = { navController.navigate(Route.RulesStep(it)) },
                onGuidedSetup = { navController.navigate(Route.RulesStep(RulesStep.entries.first(), guided = true)) },
            )
        }
        composable<Route.RulesStep> { entry ->
            val route = entry.toRoute<Route.RulesStep>()
            RulesStepScreen(
                step = route.step,
                guided = route.guided,
                onOpenLists = { navController.navigateToTopLevel(TopLevelDestination.LISTS) },
                onNext = { next -> navController.advanceRulesStep(next) },
                onDone = { navController.popBackStack<Route.Rules>(inclusive = false) },
            )
        }
        settingsDestination(permissions, actions)
        if (BuildConfig.DEBUG) {
            composable<Route.Debug> { DebugScreen() }
        }
    }
}

private fun NavGraphBuilder.settingsDestination(
    permissions: PermissionsState,
    actions: SystemActions,
) {
    composable<Route.Settings>(
        deepLinks = listOf(navDeepLink<Route.Settings>(basePath = "${DEEP_LINK_BASE}settings")),
    ) {
        SettingsScreen(
            isIgnoringBatteryOptimizations = permissions.batteryExempt,
            onRequestIgnoreBatteryOptimizations = actions.requestBatteryExemption,
        )
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

/** Replaces the current guided step with [next], so back from it returns to the hub. */
private fun NavHostController.advanceRulesStep(next: RulesStep) {
    navigate(Route.RulesStep(next, guided = true)) { popUpTo<Route.RulesStep> { inclusive = true } }
}

private fun NavHostController.leaveOnboarding() {
    navigate(Route.Recent) { popUpTo<Route.Onboarding> { inclusive = true } }
}
