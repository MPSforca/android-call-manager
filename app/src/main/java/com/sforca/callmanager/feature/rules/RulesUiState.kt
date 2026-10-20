package com.sforca.callmanager.feature.rules

import com.sforca.callmanager.core.data.rule.ScreeningTrace
import com.sforca.callmanager.core.model.UserPreferences

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
