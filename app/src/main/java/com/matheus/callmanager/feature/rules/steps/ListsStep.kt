package com.matheus.callmanager.feature.rules.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.designsystem.component.CmInfoButton
import com.matheus.callmanager.core.designsystem.component.CmOutlinedButton
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.feature.rules.ListCounts
import com.matheus.callmanager.feature.rules.RuleCatalog
import com.matheus.callmanager.feature.rules.RulesStep

/** Step 1: a read-only summary of the user's lists, which are edited on the Lists tab. */
@Composable
internal fun ListsStep(
    counts: ListCounts,
    onOpenLists: () -> Unit,
    onInfo: (ScreeningRuleId) -> Unit,
) {
    val spacing = LocalSpacing.current
    StepHeader(RulesStep.LISTS)
    ListRuleRow(ScreeningRuleId.BLOCK_LIST, counts.blocked, onInfo)
    ListRuleRow(ScreeningRuleId.ALLOW_LIST, counts.allowed, onInfo)
    ListRuleRow(ScreeningRuleId.NAME_PATTERN, counts.namePatterns, onInfo)
    CmOutlinedButton(
        text = stringResource(R.string.rules_lists_open),
        onClick = onOpenLists,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.medium),
    )
}

@Composable
private fun ListRuleRow(
    rule: ScreeningRuleId,
    count: Int,
    onInfo: (ScreeningRuleId) -> Unit,
) {
    val spacing = LocalSpacing.current
    val info = ruleInfo(rule, onInfo)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(RuleCatalog.forRule(rule).title), style = MaterialTheme.typography.titleMedium)
            Text(
                pluralStringResource(R.plurals.rules_lists_entries, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CmInfoButton(onClick = info.onClick, contentDescription = info.contentDescription)
    }
}
