package com.matheus.callmanager.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations (Navigation 2.8+). */
sealed interface Route {
    @Serializable
    data object Onboarding : Route

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

    @Serializable
    data object Debug : Route
}

/** Base URI for deep links, e.g. `callmanager://call/%2B15551234567`. */
internal const val DEEP_LINK_BASE = "callmanager://"
