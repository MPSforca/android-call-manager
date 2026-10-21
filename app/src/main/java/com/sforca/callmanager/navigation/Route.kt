package com.sforca.callmanager.navigation

import kotlinx.serialization.Serializable
import com.sforca.callmanager.feature.rules.RulesStep as RulesStepPage

/** Type-safe navigation destinations (Navigation 2.8+). */
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    /** Second first-run page: the most important rules. Only shown once, after the permissions page. */
    @Serializable
    data object OnboardingRules : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Recent : Route

    @Serializable
    data class CallDetail(
        val number: String,
    ) : Route

    @Serializable
    data object Lists : Route

    @Serializable
    data object Rules : Route

    /** One page of the Rules flow. */
    @Serializable
    data class RulesStep(
        val step: RulesStepPage,
    ) : Route

    @Serializable
    data object Settings : Route
}

/** Base URI for deep links, e.g. `callmanager://call/%2B15551234567`. */
internal const val DEEP_LINK_BASE = "callmanager://"
