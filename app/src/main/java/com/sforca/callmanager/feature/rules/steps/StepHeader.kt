package com.sforca.callmanager.feature.rules.steps

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.SectionHeader
import com.sforca.callmanager.feature.rules.RulesStep

/** Numbered title plus the plain-language intro that opens every Rules step. */
@Composable
internal fun StepHeader(step: RulesStep) {
    SectionHeader(
        title = stringResource(R.string.rules_step_header, step.ordinal + 1, stringResource(step.title)),
        subtitle = stringResource(step.intro),
    )
}
