package com.matheus.callmanager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.matheus.callmanager.core.designsystem.component.CmScaffold
import com.matheus.callmanager.core.designsystem.component.CmTopAppBar
import com.matheus.callmanager.feature.onboarding.MissingPermissionsBanner
import com.matheus.callmanager.feature.onboarding.OnboardingStep
import com.matheus.callmanager.feature.onboarding.PermissionsState
import com.matheus.callmanager.navigation.CallManagerNavHost
import com.matheus.callmanager.navigation.Route
import com.matheus.callmanager.navigation.SystemActions
import com.matheus.callmanager.navigation.TopLevelDestination
import com.matheus.callmanager.navigation.navigateToTopLevel

/**
 * App shell: a navigation bar on phones and a rail on landscape/tablet widths around the nav host.
 * The bar and top app bar only appear on the top-level tabs, not on onboarding or call detail.
 */
@Composable
fun CallManagerApp(
    permissions: PermissionsState,
    actions: SystemActions,
) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val isDebugBuild = BuildConfig.DEBUG
    val topLevel = TopLevelDestination.from(currentDestination, isDebugBuild)
    // Chosen once: later permission changes surface as a banner rather than yanking the user around.
    val startDestination = remember { if (permissions.needsOnboarding) Route.Onboarding else Route.Home }
    val layoutType =
        if (topLevel != null) {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
        } else {
            NavigationSuiteType.None
        }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.visible(isDebugBuild).forEach { destination ->
                item(
                    selected = destination.matches(currentDestination),
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
        layoutType = layoutType,
    ) {
        CmScaffold(
            topBar = { AppTopBar(navController, topLevel, currentDestination) },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Home shows its own, richer status header, so the strip would only repeat it there.
                if (topLevel != null && topLevel != TopLevelDestination.HOME && permissions.showBanner) {
                    MissingPermissionsBanner(
                        state = permissions,
                        onFix = { step ->
                            when (step) {
                                OnboardingStep.ROLE -> actions.requestScreeningRole()
                                OnboardingStep.PERMISSIONS -> actions.requestRuntimePermissions()
                                OnboardingStep.BATTERY -> actions.requestBatteryExemption()
                            }
                        },
                    )
                }
                CallManagerNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    permissions = permissions,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun AppTopBar(
    navController: NavHostController,
    topLevel: TopLevelDestination?,
    currentDestination: NavDestination?,
) {
    when {
        topLevel == TopLevelDestination.HOME ->
            CmTopAppBar(title = stringResource(R.string.app_name)) {
                IconButton(onClick = { navController.navigate(Route.Settings) }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                }
            }
        topLevel != null -> CmTopAppBar(title = stringResource(R.string.app_name))
        currentDestination?.hasRoute(Route.Settings::class) == true ->
            CmTopAppBar(
                title = stringResource(R.string.settings_title),
                onBack = { navController.popBackStack() },
                backContentDescription = stringResource(R.string.settings_back),
            )
    }
}
