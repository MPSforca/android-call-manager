package com.matheus.callmanager.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Rule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.matheus.callmanager.R
import kotlin.reflect.KClass

/** The tabs shown in the bottom bar / navigation rail. */
enum class TopLevelDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: Route,
    val routeClass: KClass<out Route>,
    val debugOnly: Boolean = false,
    private val nestedRouteClasses: List<KClass<out Route>> = emptyList(),
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Route.Home, Route.Home::class),
    RECENT(R.string.nav_recent, Icons.Filled.History, Route.Recent, Route.Recent::class),
    LISTS(R.string.nav_lists, Icons.Filled.Block, Route.Lists, Route.Lists::class),
    RULES(
        R.string.nav_rules,
        Icons.Filled.Rule,
        Route.Rules,
        Route.Rules::class,
        nestedRouteClasses = listOf(Route.RulesStep::class),
    ),
    DEBUG(R.string.nav_debug, Icons.Filled.BugReport, Route.Debug, Route.Debug::class, debugOnly = true),
    ;

    fun matches(destination: NavDestination?): Boolean =
        (listOf(routeClass) + nestedRouteClasses).any {
            destination?.hasRoute(it) ==
                true
        }

    companion object {
        fun visible(isDebugBuild: Boolean): List<TopLevelDestination> = entries.filter { isDebugBuild || !it.debugOnly }

        fun from(
            destination: NavDestination?,
            isDebugBuild: Boolean,
        ): TopLevelDestination? = visible(isDebugBuild).firstOrNull { it.matches(destination) }
    }
}
