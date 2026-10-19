package com.matheus.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmSwitchRow
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.feature.rules.RulesStep
import com.matheus.callmanager.feature.rules.RulesViewModel

/** Step 3: the fallback for numbers no earlier rule recognised. */
@Composable
internal fun UnknownCallersStep(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
) {
    StepHeader(RulesStep.UNKNOWN_CALLERS)
    CmSwitchRow(
        title = stringResource(R.string.rules_unknown_title),
        subtitle = stringResource(R.string.rules_unknown_subtitle),
        checked = prefs.blockUnknownNumbers,
        onCheckedChange = { viewModel.setBlockUnknownNumbers(it) },
    )
}
