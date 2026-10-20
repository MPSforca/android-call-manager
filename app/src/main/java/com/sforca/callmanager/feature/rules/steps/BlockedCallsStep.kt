package com.sforca.callmanager.feature.rules.steps

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.CmSegmentedButton
import com.sforca.callmanager.core.designsystem.component.CmSwitchRow
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences
import com.sforca.callmanager.feature.rules.RulesStep
import com.sforca.callmanager.feature.rules.RulesViewModel

/** Step 4: how a call that fails every allow rule reaches the user. */
@Composable
internal fun BlockedCallsStep(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
) {
    val spacing = LocalSpacing.current
    val labels = BlockedCallAction.entries.associateWith { stringResource(it.labelRes()) }
    StepHeader(RulesStep.BLOCKED_CALLS)
    CmSegmentedButton(
        options = BlockedCallAction.entries,
        selected = prefs.blockedCallAction,
        onSelect = { viewModel.setBlockedCallAction(it) },
        label = { labels.getValue(it) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
    )
    Text(
        text = stringResource(prefs.blockedCallAction.hintRes()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
    )
    CmSwitchRow(
        title = stringResource(R.string.rules_notify_title),
        subtitle = stringResource(R.string.rules_notify_subtitle),
        checked = prefs.showRejectedCallNotification,
        onCheckedChange = { viewModel.setShowRejectedCallNotification(it) },
    )
}

private fun BlockedCallAction.labelRes() =
    when (this) {
        BlockedCallAction.SILENCE -> R.string.rules_action_silence
        BlockedCallAction.REJECT -> R.string.rules_action_reject
    }

private fun BlockedCallAction.hintRes() =
    when (this) {
        BlockedCallAction.SILENCE -> R.string.rules_action_silence_hint
        BlockedCallAction.REJECT -> R.string.rules_action_reject_hint
    }
