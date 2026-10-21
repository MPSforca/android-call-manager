package com.sforca.callmanager.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sforca.callmanager.R
import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.core.designsystem.component.CmButton
import com.sforca.callmanager.core.designsystem.component.CmOutlinedButton
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.feature.rules.steps.BlockedCallsStep
import com.sforca.callmanager.feature.rules.steps.ListsStep
import com.sforca.callmanager.feature.rules.steps.PeopleYouKnowStep
import com.sforca.callmanager.feature.rules.steps.UnknownCallersStep

/** One page of the Rules flow, with a button back to the hub and Previous / Next buttons between steps. */
@Composable
fun RulesStepScreen(
    step: RulesStep,
    onOpenLists: () -> Unit,
    onBackToRules: () -> Unit,
    onNavigate: (RulesStep) -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RulesStepContent(
        step = step,
        state = state,
        viewModel = viewModel,
        onOpenLists = onOpenLists,
        onBackToRules = onBackToRules,
        onPrevious = { RulesStep.entries.getOrNull(step.ordinal - 1)?.let(onNavigate) },
        onNext = { RulesStep.entries.getOrNull(step.ordinal + 1)?.let(onNavigate) },
    )
}

/** Stateless body of [RulesStepScreen]; [viewModel] only receives the user's edits. */
@Composable
internal fun RulesStepContent(
    step: RulesStep,
    state: RulesUiState,
    viewModel: RulesViewModel,
    onOpenLists: () -> Unit,
    onBackToRules: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var infoRule by remember { mutableStateOf<ScreeningRuleId?>(null) }
    infoRule?.let { RuleInfoSheet(RuleCatalog.forRule(it), onDismiss = { infoRule = null }) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.medium)) {
        CmOutlinedButton(
            text = stringResource(R.string.rules_step_back_to_rules),
            onClick = onBackToRules,
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
        )
        val onInfo: (ScreeningRuleId) -> Unit = { infoRule = it }
        when (step) {
            RulesStep.LISTS -> ListsStep(state.listCounts, onOpenLists, onInfo)
            RulesStep.PEOPLE_YOU_KNOW -> PeopleYouKnowStep(state.preferences, viewModel, onInfo)
            RulesStep.UNKNOWN_CALLERS -> UnknownCallersStep(state.preferences, viewModel, onInfo)
            RulesStep.BLOCKED_CALLS -> BlockedCallsStep(state.preferences, viewModel)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            CmOutlinedButton(
                text = stringResource(R.string.rules_step_previous),
                onClick = onPrevious,
                enabled = step.ordinal > 0,
                modifier = Modifier.weight(1f),
            )
            CmButton(
                text = stringResource(R.string.rules_step_next),
                onClick = onNext,
                enabled = step.ordinal < RulesStep.entries.lastIndex,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
