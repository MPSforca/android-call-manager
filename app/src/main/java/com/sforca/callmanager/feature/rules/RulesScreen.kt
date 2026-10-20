package com.sforca.callmanager.feature.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.CmButton
import com.sforca.callmanager.core.designsystem.component.CmCard
import com.sforca.callmanager.core.designsystem.component.CmOutlinedButton
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/** The Rules hub: how rules are evaluated, one status card per step, and a guided walkthrough. */
@Composable
fun RulesScreen(
    onOpenStep: (RulesStep) -> Unit,
    onGuidedSetup: () -> Unit,
    onTestNumber: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RulesHubContent(
        state = state,
        actions =
            RulesHubActions(
                onOpenStep = onOpenStep,
                onGuidedSetup = onGuidedSetup,
                onTestNumber = onTestNumber,
                onPreviewChange = { number, name -> viewModel.setPreview(number, name) },
            ),
    )
}

/** What the user can do on the hub, grouped so the stateless body stays readable. */
internal data class RulesHubActions(
    val onOpenStep: (RulesStep) -> Unit,
    val onGuidedSetup: () -> Unit,
    val onTestNumber: () -> Unit,
    val onPreviewChange: (number: String, callerName: String?) -> Unit,
)

/** Stateless body of [RulesScreen]. */
@Composable
internal fun RulesHubContent(
    state: RulesUiState,
    actions: RulesHubActions,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.rules_hub_intro),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
        )
        CmButton(
            text = stringResource(R.string.rules_hub_guided_setup),
            onClick = actions.onGuidedSetup,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
        )
        CmOutlinedButton(
            text = stringResource(R.string.rules_hub_test_number),
            onClick = actions.onTestNumber,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
        )
        RulesStep.entries.forEach { step ->
            StepCard(
                step = step,
                summary = summarize(step, state.preferences, state.listCounts).text(),
                onClick = { actions.onOpenStep(step) },
            )
        }
        RulePreviewCard(state.preview, actions.onPreviewChange)
    }
}

@Composable
private fun StepCard(
    step: RulesStep,
    summary: String,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    CmCard(
        modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small).clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = (step.ordinal + 1).toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(step.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
