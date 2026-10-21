package com.sforca.callmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.sforca.callmanager.feature.debug.DebugScreen
import com.sforca.callmanager.feature.home.HomeScreen
import com.sforca.callmanager.feature.home.RulesHint
import com.sforca.callmanager.feature.lists.ListsScreen
import com.sforca.callmanager.feature.onboarding.OnboardingRulesScreen
import com.sforca.callmanager.feature.onboarding.OnboardingRulesViewModel
import com.sforca.callmanager.feature.onboarding.OnboardingScreen
import com.sforca.callmanager.feature.onboarding.OnboardingStep
import com.sforca.callmanager.feature.onboarding.PermissionsState
import com.sforca.callmanager.feature.recent.CallDetailRoute
import com.sforca.callmanager.feature.recent.RecentScreen
import com.sforca.callmanager.feature.rules.RulesScreen
import com.sforca.callmanager.feature.rules.RulesStep
import com.sforca.callmanager.feature.rules.RulesStepScreen
import com.sforca.callmanager.feature.settings.SettingsScreen

@Composable
fun CallManagerNavHost(
    navController: NavHostController,
    startDestination: Route,
    permissions: PermissionsState,
    actions: SystemActions,
    modifier: Modifier = Modifier,
) {
    // One-shot "more rules live in the Rules tab" note shown on Home right after first-run setup.
    var showRulesHint by rememberSaveable { mutableStateOf(false) }
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        onboardingDestinations(navController, permissions, actions, onRulesSetupDone = { showRulesHint = true })
        composable<Route.Home>(deepLinks = listOf(navDeepLink<Route.Home>(basePath = "${DEEP_LINK_BASE}home"))) {
            HomeScreen(
                permissions = permissions,
                onFix = { step ->
                    when (step) {
                        OnboardingStep.ROLE -> actions.requestScreeningRole()
                        OnboardingStep.PERMISSIONS -> actions.requestRuntimePermissions()
                        OnboardingStep.BATTERY -> actions.requestBatteryExemption()
                    }
                },
                onCallSelected = { navController.navigate(Route.CallDetail(it)) },
                onSeeAll = { navController.navigateToTopLevel(TopLevelDestination.RECENT) },
                rulesHint =
                    if (showRulesHint) {
                        RulesHint(
                            onDismiss = { showRulesHint = false },
                            onOpen = {
                                showRulesHint = false
                                navController.navigateToTopLevel(TopLevelDestination.RULES)
                            },
                        )
                    } else {
                        null
                    },
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
        rulesDestinations(navController)
        settingsDestination(permissions, actions)
        composable<Route.TestNumber>(
            deepLinks = listOf(navDeepLink<Route.TestNumber>(basePath = "${DEEP_LINK_BASE}test")),
        ) { DebugScreen() }
    }
}

private fun NavGraphBuilder.onboardingDestinations(
    navController: NavHostController,
    permissions: PermissionsState,
    actions: SystemActions,
    onRulesSetupDone: () -> Unit,
) {
    composable<Route.Onboarding> {
        val rulesViewModel: OnboardingRulesViewModel = hiltViewModel()
        // The rules page is first-run only, so a returning user goes straight to Home.
        val leave = {
            rulesViewModel.needsRulesSetup { needed ->
                if (needed) navController.openOnboardingRules() else navController.leaveOnboarding()
            }
        }
        OnboardingScreen(
            state = permissions,
            onRequestRole = actions.requestScreeningRole,
            onRequestPermissions = actions.requestRuntimePermissions,
            onRequestBatteryExemption = actions.requestBatteryExemption,
            onSkip = {
                actions.skipOnboarding()
                leave()
            },
            onContinue = { leave() },
        )
    }
    composable<Route.OnboardingRules> {
        val viewModel: OnboardingRulesViewModel = hiltViewModel()
        val preferences by viewModel.preferences.collectAsStateWithLifecycle()
        OnboardingRulesScreen(
            preferences = preferences,
            onBlockUnknownChange = { viewModel.setBlockUnknownNumbers(it) },
            onActionChange = { viewModel.setBlockedCallAction(it) },
            onFinish = {
                viewModel.finish {
                    onRulesSetupDone()
                    navController.leaveOnboardingRules()
                }
            },
        )
    }
}

private fun NavGraphBuilder.rulesDestinations(navController: NavHostController) {
    composable<Route.Rules>(deepLinks = listOf(navDeepLink<Route.Rules>(basePath = "${DEEP_LINK_BASE}rules"))) {
        RulesScreen(
            onOpenStep = { navController.navigate(Route.RulesStep(it)) },
            onGuidedSetup = { navController.navigate(Route.RulesStep(RulesStep.entries.first(), guided = true)) },
            onTestNumber = { navController.navigateToTopLevel(TopLevelDestination.TEST_NUMBER) },
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
    popUpTo<Route.Home> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Replaces the current guided step with [next], so back from it returns to the hub. */
private fun NavHostController.advanceRulesStep(next: RulesStep) {
    navigate(Route.RulesStep(next, guided = true)) { popUpTo<Route.RulesStep> { inclusive = true } }
}

private fun NavHostController.openOnboardingRules() {
    navigate(Route.OnboardingRules) { popUpTo<Route.Onboarding> { inclusive = true } }
}

private fun NavHostController.leaveOnboardingRules() {
    navigate(Route.Home) { popUpTo<Route.OnboardingRules> { inclusive = true } }
}

private fun NavHostController.leaveOnboarding() {
    navigate(Route.Home) { popUpTo<Route.Onboarding> { inclusive = true } }
}
