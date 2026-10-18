package com.matheus.callmanager.core.data.rule

import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.model.UserPreferences

/** Everything a rule needs to decide on one incoming call; [nowMillis] is injected to keep rules deterministic. */
data class ScreeningContext(
    val number: String,
    val callerName: String?,
    val preferences: UserPreferences,
    val nowMillis: Long,
)

/**
 * One step of the screening chain. Returns a decision to stop the chain, or null to defer to the next rule.
 * Precedence is the order rules appear in [ScreeningRules] — keep it in sync with CLAUDE.md.
 */
fun interface ScreeningRule {
    suspend fun evaluate(context: ScreeningContext): ScreeningDecision?
}
