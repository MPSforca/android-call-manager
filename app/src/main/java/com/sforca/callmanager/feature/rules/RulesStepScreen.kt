package com.sforca.callmanager.feature.rules

import androidx.compose.foundation.layout.Column
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
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.feature.rules.steps.BlockedCallsStep
import com.sforca.callmanager.feature.rules.steps.ListsStep
import com.sforca.callmanager.feature.rules.steps.PeopleYouKnowStep
import com.sforca.callmanager.feature.rules.steps.UnknownCallersStep

/**
 * One page of the Rules flow. In [guided] mode a Next button (Done on the last step) walks the user
 * through the steps in order.
 */
@Composable
fun RulesStepScreen(
    step: RulesStep,
    guided: Boolean,
    onOpenLists: () -> Unit,
    onNext: (RulesStep) -> Unit,
    onDone: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RulesStepContent(
        step = step,
        state = state,
        viewModel = viewModel,
        guided = guided,
        onOpenLists = onOpenLists,
        onAdvance = { RulesStep.entries.getOrNull(step.ordinal + 1)?.let(onNext) ?: onDone() },
    )
}

/** Stateless body of [RulesStepScreen]; [viewModel] only receives the user's edits. */
@Composable
internal fun RulesStepContent(
    step: RulesStep,
    state: RulesUiState,
    viewModel: RulesViewModel,
    guided: Boolean,
    onOpenLists: () -> Unit,
    onAdvance: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var infoRule by remember { mutableStateOf<ScreeningRuleId?>(null) }
    infoRule?.let { RuleInfoSheet(RuleCatalog.forRule(it), onDismiss = { infoRule = null }) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.medium)) {
        val onInfo: (ScreeningRuleId) -> Unit = { infoRule = it }
        when (step) {
            RulesStep.LISTS -> ListsStep(state.listCounts, onOpenLists, onInfo)
            RulesStep.PEOPLE_YOU_KNOW -> PeopleYouKnowStep(state.preferences, viewModel, onInfo)
            RulesStep.UNKNOWN_CALLERS -> UnknownCallersStep(state.preferences, viewModel, onInfo)
            RulesStep.BLOCKED_CALLS -> BlockedCallsStep(state.preferences, viewModel)
        }
        RulePreviewCard(state.preview, onChange = { number, name -> viewModel.setPreview(number, name) })
        if (guided) {
            val last = step.ordinal == RulesStep.entries.lastIndex
            CmButton(
                text = stringResource(if (last) R.string.rules_step_done else R.string.rules_step_next),
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.medium),
            )
        }
    }
}
