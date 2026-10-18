package com.matheus.callmanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlin.reflect.KClass

/** The tabs shown in the bottom bar / navigation rail. */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Route,
    val routeClass: KClass<out Route>,
    val debugOnly: Boolean = false,
) {
    RECENT("Recent", Icons.Filled.History, Route.Recent, Route.Recent::class),
    LISTS("Lists", Icons.Filled.Block, Route.Lists, Route.Lists::class),
    RULES("Rules", Icons.Filled.Rule, Route.Rules, Route.Rules::class),
    DEBUG("Debug", Icons.Filled.BugReport, Route.Debug, Route.Debug::class, debugOnly = true),
    ;

    fun matches(destination: NavDestination?): Boolean = destination?.hasRoute(routeClass) == true

    companion object {
        fun visible(isDebugBuild: Boolean): List<TopLevelDestination> = entries.filter { isDebugBuild || !it.debugOnly }

        fun from(
            destination: NavDestination?,
            isDebugBuild: Boolean,
        ): TopLevelDestination? = visible(isDebugBuild).firstOrNull { it.matches(destination) }
    }
}
