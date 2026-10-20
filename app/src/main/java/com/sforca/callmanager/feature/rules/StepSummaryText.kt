package com.sforca.callmanager.feature.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.model.BlockedCallAction

/** The one-line status shown on a hub card. */
@Composable
internal fun StepSummary.text(): String =
    when (this) {
        is StepSummary.Lists ->
            stringResource(R.string.rules_summary_lists, counts.blocked, counts.allowed, counts.namePatterns)
        is StepSummary.Toggles -> stringResource(R.string.rules_summary_toggles, enabled, total)
        is StepSummary.UnknownCallers ->
            stringResource(if (rejected) R.string.rules_summary_unknown_on else R.string.rules_summary_unknown_off)
        is StepSummary.BlockedCalls ->
            stringResource(
                when {
                    action == BlockedCallAction.SILENCE -> R.string.rules_summary_blocked_silence
                    notifies -> R.string.rules_summary_blocked_reject_notify
                    else -> R.string.rules_summary_blocked_reject
                },
            )
    }
