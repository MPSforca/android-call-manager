package com.matheus.callmanager.feature.rules

import androidx.annotation.StringRes
import com.matheus.callmanager.R
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.UserPreferences

/** The pages of the Rules flow, in the order the user walks through them. */
enum class RulesStep(
    @StringRes val title: Int,
    @StringRes val intro: Int,
) {
    LISTS(R.string.rules_step_lists_title, R.string.rules_step_lists_intro),
    PEOPLE_YOU_KNOW(R.string.rules_step_people_title, R.string.rules_step_people_intro),
    UNKNOWN_CALLERS(R.string.rules_step_unknown_title, R.string.rules_step_unknown_intro),
    BLOCKED_CALLS(R.string.rules_step_blocked_title, R.string.rules_step_blocked_intro),
}

/**
 * Plain-language explanation of one rule in the screening chain. [order] is its 1-based position in
 * the chain (first match wins); [step] is the page that configures it.
 */
data class RuleDescriptor(
    val id: ScreeningRuleId,
    val order: Int,
    val step: RulesStep,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val example: Int,
)

/** Single source of rule wording, shared by the Rules hub, the info sheets and the trace. */
object RuleCatalog {
    /** One descriptor per [ScreeningRuleId], in chain order. */
    val descriptors: List<RuleDescriptor> =
        listOf(
            describe(
                ScreeningRuleId.BLOCK_LIST,
                RulesStep.LISTS,
                R.string.rules_info_block_list_title,
                R.string.rules_info_block_list_description,
                R.string.rules_info_block_list_example,
            ),
            describe(
                ScreeningRuleId.ALLOW_LIST,
                RulesStep.LISTS,
                R.string.rules_info_allow_list_title,
                R.string.rules_info_allow_list_description,
                R.string.rules_info_allow_list_example,
            ),
            describe(
                ScreeningRuleId.NAME_PATTERN,
                RulesStep.LISTS,
                R.string.rules_info_name_pattern_title,
                R.string.rules_info_name_pattern_description,
                R.string.rules_info_name_pattern_example,
            ),
            describe(
                ScreeningRuleId.CONTACT,
                RulesStep.PEOPLE_YOU_KNOW,
                R.string.rules_info_contact_title,
                R.string.rules_info_contact_description,
                R.string.rules_info_contact_example,
            ),
            describe(
                ScreeningRuleId.RECENTLY_ACCEPTED,
                RulesStep.PEOPLE_YOU_KNOW,
                R.string.rules_info_recently_accepted_title,
                R.string.rules_info_recently_accepted_description,
                R.string.rules_info_recently_accepted_example,
            ),
            describe(
                ScreeningRuleId.RECENTLY_CALLED,
                RulesStep.PEOPLE_YOU_KNOW,
                R.string.rules_info_recently_called_title,
                R.string.rules_info_recently_called_description,
                R.string.rules_info_recently_called_example,
            ),
            describe(
                ScreeningRuleId.UNKNOWN_FALLBACK,
                RulesStep.UNKNOWN_CALLERS,
                R.string.rules_info_unknown_title,
                R.string.rules_info_unknown_description,
                R.string.rules_info_unknown_example,
            ),
        )

    fun forRule(id: ScreeningRuleId): RuleDescriptor = descriptors.first { it.id == id }

    fun forStep(step: RulesStep): List<RuleDescriptor> = descriptors.filter { it.step == step }

    private fun describe(
        id: ScreeningRuleId,
        step: RulesStep,
        @StringRes title: Int,
        @StringRes description: Int,
        @StringRes example: Int,
    ) = RuleDescriptor(id, id.ordinal + 1, step, title, description, example)
}

/** How many entries each list holds, for the "Your lists" step summary. */
data class ListCounts(
    val blocked: Int = 0,
    val allowed: Int = 0,
    val namePatterns: Int = 0,
)

/** What the hub shows on a step's card. The UI turns it into text; this stays free of resources. */
sealed interface StepSummary {
    data class Lists(
        val counts: ListCounts,
    ) : StepSummary

    /** [enabled] of [total] toggles are on. */
    data class Toggles(
        val enabled: Int,
        val total: Int,
    ) : StepSummary

    data class UnknownCallers(
        val rejected: Boolean,
    ) : StepSummary

    data class BlockedCalls(
        val action: BlockedCallAction,
        val notifies: Boolean,
    ) : StepSummary
}

fun summarize(
    step: RulesStep,
    prefs: UserPreferences,
    listCounts: ListCounts = ListCounts(),
): StepSummary =
    when (step) {
        RulesStep.LISTS -> StepSummary.Lists(listCounts)
        RulesStep.PEOPLE_YOU_KNOW ->
            StepSummary.Toggles(
                enabled =
                    listOf(prefs.allowContacts, prefs.recentAcceptedRuleEnabled, prefs.recentOutgoingRuleEnabled)
                        .count { it },
                total = 3,
            )
        RulesStep.UNKNOWN_CALLERS -> StepSummary.UnknownCallers(prefs.blockUnknownNumbers)
        RulesStep.BLOCKED_CALLS ->
            StepSummary.BlockedCalls(
                action = prefs.blockedCallAction,
                // The notification only exists for outright rejections.
                notifies = prefs.blockedCallAction == BlockedCallAction.REJECT && prefs.showRejectedCallNotification,
            )
    }
