package com.sforca.callmanager.core.data.rule

import com.sforca.callmanager.core.model.ScreeningDecision
import com.sforca.callmanager.core.model.UserPreferences

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

/** Stable identity of each rule in the chain, so UI can label trace steps without depending on rule classes. */
enum class ScreeningRuleId {
    BLOCK_LIST,
    ALLOW_LIST,
    NAME_PATTERN,
    CONTACT,
    RECENTLY_ACCEPTED,
    RECENTLY_CALLED,
    UNKNOWN_FALLBACK,
}

enum class TraceOutcome {
    /** The rule ran and deferred to the next one. */
    PASSED,

    /** The rule ran and produced the final decision. */
    DECIDED,

    /** An earlier rule decided, so this one never ran. */
    NOT_REACHED,
}

data class TraceStep(
    val rule: ScreeningRuleId,
    val outcome: TraceOutcome,
)

/** The verdict for a call plus how every rule in the chain contributed to it. */
data class ScreeningTrace(
    val decision: ScreeningDecision,
    val steps: List<TraceStep>,
)
