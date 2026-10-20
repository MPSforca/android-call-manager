package com.sforca.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.feature.rules.RuleCatalog

/** Info-button arguments for [com.sforca.callmanager.core.designsystem.component.CmSwitchRow]. */
internal data class RuleInfo(
    val onClick: () -> Unit,
    val contentDescription: String,
)

@Composable
internal fun ruleInfo(
    rule: ScreeningRuleId,
    onInfo: (ScreeningRuleId) -> Unit,
): RuleInfo {
    val title = stringResource(RuleCatalog.forRule(rule).title)
    val description = stringResource(R.string.rules_info_action, title)
    return remember(rule, description, onInfo) { RuleInfo({ onInfo(rule) }, description) }
}
