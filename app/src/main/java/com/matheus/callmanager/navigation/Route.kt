package com.matheus.callmanager.navigation

import kotlinx.serialization.Serializable
import com.matheus.callmanager.feature.rules.RulesStep as RulesStepPage

/** Type-safe navigation destinations (Navigation 2.8+). */
sealed interface Route {
    @Serializable
    data object Onboarding : Route

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

    /** One page of the Rules flow; [guided] shows a Next/Done button that walks to the following step. */
    @Serializable
    data class RulesStep(
        val step: RulesStepPage,
        val guided: Boolean = false,
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object TestNumber : Route
}

/** Base URI for deep links, e.g. `callmanager://call/%2B15551234567`. */
internal const val DEEP_LINK_BASE = "callmanager://"
