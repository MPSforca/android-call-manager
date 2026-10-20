package com.matheus.callmanager.feature.rules

import com.matheus.callmanager.core.model.UserPreferences

/** Everything the Rules hub and step pages render. */
data class RulesUiState(
    val preferences: UserPreferences = UserPreferences(),
    val listCounts: ListCounts = ListCounts(),
)
