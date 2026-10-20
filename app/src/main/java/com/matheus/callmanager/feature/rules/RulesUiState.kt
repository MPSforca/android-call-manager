package com.matheus.callmanager.feature.rules

import com.matheus.callmanager.core.data.rule.ScreeningTrace
import com.matheus.callmanager.core.model.UserPreferences

/** The verdict the current rules give for [number]; it is recomputed whenever a rule changes. */
data class RulePreview(
    val number: String,
    val trace: ScreeningTrace,
)

/** Everything the Rules hub and step pages render. */
data class RulesUiState(
    val preferences: UserPreferences = UserPreferences(),
    val listCounts: ListCounts = ListCounts(),
    val preview: RulePreview? = null,
)
