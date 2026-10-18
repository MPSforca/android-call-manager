package com.matheus.callmanager.feature.rules

import com.matheus.callmanager.core.model.UserPreferences

/** Everything [RulesScreen] renders. */
data class RulesUiState(
    val preferences: UserPreferences = UserPreferences(),
)
