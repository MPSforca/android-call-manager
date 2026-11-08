package com.sforca.callmanager.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
import com.sforca.callmanager.core.designsystem.component.CmCard
import com.sforca.callmanager.core.designsystem.component.CmOutlinedButton
import com.sforca.callmanager.core.designsystem.component.CmTextButton
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

private class StepContent(
    @StringRes val title: Int,
    @StringRes val why: Int,
    @StringRes val action: Int,
    val optional: Boolean = false,
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
                optional = true,
            )
    }

/** Everything the first-run permissions page can ask its host to do. */
data class OnboardingActions(
    val onRequestRole: () -> Unit,
    val onRequestPermissions: () -> Unit,
    val onRequestBatteryExemption: () -> Unit,
    val onSkip: () -> Unit,
    val onContinue: () -> Unit,
    val onOpenPrivacy: () -> Unit,
)

/**
 * First-run permissions page: every access Call Manager needs on one screen, each with its reason
 * and a live done/enable state. Continue unlocks once the core steps (role, permissions) are done;
 * battery exemption stays optional. Stateless; [state] drives what each row shows.
 */
@Composable
fun OnboardingScreen(
    state: PermissionsState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)
        OnboardingStep.entries.forEach { step ->
            PermissionRow(
                content = contentFor(step),
                done = state.isDone(step),
                onAction =
                    when (step) {
                        OnboardingStep.ROLE -> actions.onRequestRole
                        OnboardingStep.PERMISSIONS -> actions.onRequestPermissions
                        OnboardingStep.BATTERY -> actions.onRequestBatteryExemption
                    },
            )
        }
        CmButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = actions.onContinue,
            enabled = state.coreComplete,
            modifier = Modifier.fillMaxWidth(),
        )
        CmTextButton(
            text = stringResource(R.string.onboarding_skip),
            onClick = actions.onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        CmTextButton(
            text = stringResource(R.string.onboarding_privacy),
            onClick = actions.onOpenPrivacy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PermissionRow(
    content: StepContent,
    done: Boolean,
    onAction: () -> Unit,
) {
    val spacing = LocalSpacing.current
    CmCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                stringResource(content.title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.onboarding_done),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else if (content.optional) {
                Text(
                    stringResource(R.string.onboarding_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(content.why),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = spacing.small),
        )
        if (!done) {
            CmOutlinedButton(
                text = stringResource(content.action),
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
