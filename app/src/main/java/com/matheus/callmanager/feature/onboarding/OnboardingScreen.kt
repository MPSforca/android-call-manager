package com.matheus.callmanager.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmButton
import com.matheus.callmanager.core.designsystem.component.CmTextButton
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing

private class StepContent(
    @StringRes val title: Int,
    @StringRes val why: Int,
    @StringRes val action: Int,
)

private fun contentFor(step: OnboardingStep) =
    when (step) {
        OnboardingStep.ROLE ->
            StepContent(
                title = R.string.onboarding_role_title,
                why = R.string.onboarding_role_why,
                action = R.string.onboarding_role_action,
            )
        OnboardingStep.PERMISSIONS ->
            StepContent(
                title = R.string.onboarding_permissions_title,
                why = R.string.onboarding_permissions_why,
                action = R.string.onboarding_permissions_action,
            )
        OnboardingStep.BATTERY ->
            StepContent(
                title = R.string.onboarding_battery_title,
                why = R.string.onboarding_battery_why,
                action = R.string.onboarding_battery_action,
            )
    }

/**
 * First-run flow: one step at a time (screening role, runtime permissions, battery exemption),
 * each explaining why it's needed. Stateless; [state] drives which step is shown.
 */
@Composable
fun OnboardingScreen(
    state: PermissionsState,
    onRequestRole: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onSkip: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.nextStep
    LaunchedEffect(step) {
        if (step == null) onFinished()
    }
    if (step == null) return

    val spacing = LocalSpacing.current
    val content = contentFor(step)
    val stepNumber = step.ordinal + 1
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.onboarding_step_counter, stepNumber, OnboardingStep.entries.size),
            style = MaterialTheme.typography.labelLarge,
        )
        LinearProgressIndicator(
            progress = { stepNumber / OnboardingStep.entries.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(content.title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(content.why), style = MaterialTheme.typography.bodyLarge)
        CmButton(
            text = stringResource(content.action),
            onClick =
                when (step) {
                    OnboardingStep.ROLE -> onRequestRole
                    OnboardingStep.PERMISSIONS -> onRequestPermissions
                    OnboardingStep.BATTERY -> onRequestBatteryExemption
                },
            modifier = Modifier.fillMaxWidth(),
        )
        // The battery step is optional once the core steps are done, so skipping just enters the app.
        CmTextButton(
            text =
                stringResource(
                    if (step == OnboardingStep.BATTERY) R.string.onboarding_not_now else R.string.onboarding_skip,
                ),
            onClick = if (step == OnboardingStep.BATTERY) onFinished else onSkip,
        )
    }
}
