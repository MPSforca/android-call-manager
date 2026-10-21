package com.sforca.callmanager.feature.rules

import com.sforca.callmanager.core.model.UserPreferences

/** Everything the Rules hub and step pages render. */
data class RulesUiState(
    val preferences: UserPreferences = UserPreferences(),
    val listCounts: ListCounts = ListCounts(),
)
