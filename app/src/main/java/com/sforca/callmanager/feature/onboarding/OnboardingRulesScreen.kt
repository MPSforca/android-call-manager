package com.sforca.callmanager.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.CmButton
import com.sforca.callmanager.core.designsystem.component.CmSegmentedButton
import com.sforca.callmanager.core.designsystem.component.CmSwitchRow
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences

/**
 * Second first-run page: the two rules that matter most, blocking unknown callers and what happens
 * to a blocked call. Stateless; changes apply immediately, [onFinish] just leaves the page.
 */
@Composable
fun OnboardingRulesScreen(
    preferences: UserPreferences,
    onBlockUnknownChange: (Boolean) -> Unit,
    onActionChange: (BlockedCallAction) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val labels =
        BlockedCallAction.entries.associateWith {
            stringResource(
                if (it ==
                    BlockedCallAction.SILENCE
                ) {
                    R.string.rules_action_silence
                } else {
                    R.string.rules_action_reject
                },
            )
        }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.onboarding_rules_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.onboarding_rules_intro), style = MaterialTheme.typography.bodyLarge)
        CmSwitchRow(
            title = stringResource(R.string.rules_unknown_title),
            subtitle = stringResource(R.string.rules_unknown_subtitle),
            checked = preferences.blockUnknownNumbers,
            onCheckedChange = onBlockUnknownChange,
        )
        BlockedActionChoice(preferences.blockedCallAction, onActionChange)
        CmButton(
            text = stringResource(R.string.onboarding_rules_finish),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BlockedActionChoice(
    selected: BlockedCallAction,
    onSelect: (BlockedCallAction) -> Unit,
) {
    val spacing = LocalSpacing.current
    val labels =
        BlockedCallAction.entries.associateWith {
            stringResource(
                if (it == BlockedCallAction.SILENCE) R.string.rules_action_silence else R.string.rules_action_reject,
            )
        }
    Text(
        stringResource(R.string.onboarding_rules_action_label),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = spacing.large).semantics { heading() },
    )
    CmSegmentedButton(
        options = BlockedCallAction.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
    )
    Text(
        stringResource(
            if (selected == BlockedCallAction.SILENCE) {
                R.string.rules_action_silence_hint
            } else {
                R.string.rules_action_reject_hint
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = spacing.large),
    )
}
