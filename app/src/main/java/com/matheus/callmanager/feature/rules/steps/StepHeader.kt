package com.matheus.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.feature.rules.RulesStep

/** Numbered title plus the plain-language intro that opens every Rules step. */
@Composable
internal fun StepHeader(step: RulesStep) {
    SectionHeader(
        title = stringResource(R.string.rules_step_header, step.ordinal + 1, stringResource(step.title)),
        subtitle = stringResource(step.intro),
    )
}
