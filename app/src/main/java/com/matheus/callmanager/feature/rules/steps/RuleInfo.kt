package com.matheus.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.feature.rules.RuleCatalog

/** Info-button arguments for [com.matheus.callmanager.core.designsystem.component.CmSwitchRow]. */
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
