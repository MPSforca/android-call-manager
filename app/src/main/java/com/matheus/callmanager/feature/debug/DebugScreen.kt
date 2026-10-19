package com.matheus.callmanager.feature.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.R
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.data.rule.TraceOutcome
import com.matheus.callmanager.core.data.rule.TraceStep
import com.matheus.callmanager.core.designsystem.component.CmButton
import com.matheus.callmanager.core.designsystem.component.CmCard
import com.matheus.callmanager.core.designsystem.component.CmPhoneTextField
import com.matheus.callmanager.core.designsystem.component.CmTextButton
import com.matheus.callmanager.core.designsystem.component.EmptyState
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.core.designsystem.component.StatusPill
import com.matheus.callmanager.core.designsystem.component.StatusTone
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter

/** "Test a number": runs the same rule evaluation the real CallScreeningService uses, without a real call. */
@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DebugContent(
        state = state,
        onSimulate = { displayed, name ->
            val region = PhoneNumberFormatter.defaultRegion(context)
            viewModel.simulateIncomingCall(
                PhoneNumberFormatter.normalizeForStorage(context, displayed, region),
                name,
            )
        },
        onClearHistory = viewModel::clearHistory,
    )
}

/** Stateless body of [DebugScreen]; the form fields are local because they are scratch input. */
@Composable
internal fun DebugContent(
    state: DebugUiState,
    onSimulate: (number: String, callerName: String?) -> Unit,
    onClearHistory: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var number by rememberSaveable { mutableStateOf("") }
    var callerName by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        SectionHeader(
            title = stringResource(R.string.debug_title),
            subtitle = stringResource(R.string.debug_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            CmPhoneTextField(
                value = number,
                onValueChange = { number = it },
                label = stringResource(R.string.debug_number_label),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = callerName,
                onValueChange = { callerName = it },
                label = { Text(stringResource(R.string.debug_caller_name_label)) },
                placeholder = { Text(stringResource(R.string.debug_caller_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CmButton(
                text = stringResource(R.string.debug_simulate),
                onClick = { onSimulate(number, callerName.ifBlank { null }) },
                enabled = number.isNotBlank(),
            )
        }
        if (state.trace == null) {
            EmptyState(
                icon = Icons.Filled.Science,
                title = stringResource(R.string.debug_empty_title),
                message = stringResource(R.string.debug_empty_message),
            )
        }
        state.trace?.let { TraceCard(it.decision, it.steps) }
        if (state.history.isNotEmpty()) {
            HistorySection(
                history = state.history,
                onSelect = {
                    number = it.number
                    callerName = it.callerName.orEmpty()
                },
                onClear = onClearHistory,
            )
        }
    }
}

@Composable
private fun TraceCard(
    decision: ScreeningDecision,
    steps: List<TraceStep>,
) {
    val spacing = LocalSpacing.current
    CmCard(modifier = Modifier.padding(horizontal = spacing.large)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            DecisionPill(decision)
            Text(decision.reason(), style = MaterialTheme.typography.titleSmall)
        }
        Text(
            stringResource(R.string.debug_trace_title),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = spacing.medium),
        )
        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(step.rule.label()), style = MaterialTheme.typography.bodyMedium)
                StatusPill(stringResource(step.outcome.label()), step.outcome.tone(decision))
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<SimulationEntry>,
    onSelect: (SimulationEntry) -> Unit,
    onClear: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionHeader(title = stringResource(R.string.debug_history_title), modifier = Modifier.weight(1f))
        CmTextButton(text = stringResource(R.string.debug_history_clear), onClick = onClear)
    }
    history.forEach { entry ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(entry) }
                    .padding(horizontal = spacing.large, vertical = spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.number, style = MaterialTheme.typography.bodyLarge)
                Text(
                    entry.trace.decision.reason(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DecisionPill(entry.trace.decision)
        }
    }
}

@Composable
private fun DecisionPill(decision: ScreeningDecision) {
    when (decision) {
        is ScreeningDecision.Allow -> StatusPill(stringResource(R.string.debug_allowed), StatusTone.ALLOWED)
        is ScreeningDecision.Reject -> StatusPill(stringResource(R.string.debug_rejected), StatusTone.BLOCKED)
    }
}

private fun ScreeningDecision.reason(): String =
    when (this) {
        is ScreeningDecision.Allow -> reason
        is ScreeningDecision.Reject -> reason
    }

private fun TraceOutcome.tone(decision: ScreeningDecision): StatusTone =
    when (this) {
        TraceOutcome.PASSED, TraceOutcome.NOT_REACHED -> StatusTone.UNKNOWN
        TraceOutcome.DECIDED -> if (decision is ScreeningDecision.Allow) StatusTone.ALLOWED else StatusTone.BLOCKED
    }

@StringRes
private fun TraceOutcome.label(): Int =
    when (this) {
        TraceOutcome.PASSED -> R.string.debug_outcome_passed
        TraceOutcome.DECIDED -> R.string.debug_outcome_decided
        TraceOutcome.NOT_REACHED -> R.string.debug_outcome_not_reached
    }

@StringRes
private fun ScreeningRuleId.label(): Int =
    when (this) {
        ScreeningRuleId.BLOCK_LIST -> R.string.debug_rule_block_list
        ScreeningRuleId.ALLOW_LIST -> R.string.debug_rule_allow_list
        ScreeningRuleId.NAME_PATTERN -> R.string.debug_rule_name_pattern
        ScreeningRuleId.CONTACT -> R.string.debug_rule_contact
        ScreeningRuleId.RECENTLY_ACCEPTED -> R.string.debug_rule_recently_accepted
        ScreeningRuleId.RECENTLY_CALLED -> R.string.debug_rule_recently_called
        ScreeningRuleId.UNKNOWN_FALLBACK -> R.string.debug_rule_unknown
    }
