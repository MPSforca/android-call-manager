package com.sforca.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.core.designsystem.component.CmSwitchRow
import com.sforca.callmanager.core.model.UserPreferences
import com.sforca.callmanager.feature.rules.RulesStep
import com.sforca.callmanager.feature.rules.RulesViewModel

/** Step 3: the fallback for numbers no earlier rule recognised. */
@Composable
internal fun UnknownCallersStep(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
    onInfo: (ScreeningRuleId) -> Unit,
) {
    val info = ruleInfo(ScreeningRuleId.UNKNOWN_FALLBACK, onInfo)
    StepHeader(RulesStep.UNKNOWN_CALLERS)
    CmSwitchRow(
        title = stringResource(R.string.rules_unknown_title),
        subtitle = stringResource(R.string.rules_unknown_subtitle),
        checked = prefs.blockUnknownNumbers,
        onCheckedChange = { viewModel.setBlockUnknownNumbers(it) },
        onInfoClick = info.onClick,
        infoContentDescription = info.contentDescription,
    )
}
