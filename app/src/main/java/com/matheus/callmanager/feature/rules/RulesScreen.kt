package com.matheus.callmanager.feature.rules

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmButton
import com.matheus.callmanager.core.designsystem.component.CmCard
import com.matheus.callmanager.core.designsystem.component.CmSwitchRow
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.feature.rules.steps.BlockedCallsStep
import com.matheus.callmanager.feature.rules.steps.PeopleYouKnowStep
import com.matheus.callmanager.feature.rules.steps.UnknownCallersStep

@Composable
fun RulesScreen(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RulesContent(
        state = state,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
        onRequestIgnoreBatteryOptimizations = onRequestIgnoreBatteryOptimizations,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        viewModel = viewModel,
    )
}

/** Stateless body of [RulesScreen]; [viewModel] only receives the user's edits. */
@Composable
internal fun RulesContent(
    state: RulesUiState,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    dynamicColorSupported: Boolean,
    viewModel: RulesViewModel,
) {
    val prefs = state.preferences
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = LocalSpacing.current.medium),
    ) {
        if (!isIgnoringBatteryOptimizations) {
            BatteryCard(onRequestIgnoreBatteryOptimizations)
        }
        PeopleYouKnowStep(prefs, viewModel)
        UnknownCallersStep(prefs, viewModel)
        BlockedCallsStep(prefs, viewModel)
        if (dynamicColorSupported) {
            SectionHeader(title = stringResource(R.string.rules_section_appearance))
            CmSwitchRow(
                title = stringResource(R.string.rules_dynamic_color_title),
                subtitle = stringResource(R.string.rules_dynamic_color_subtitle),
                checked = prefs.dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )
        }
    }
}

@Composable
private fun BatteryCard(onRequest: () -> Unit) {
    val spacing = LocalSpacing.current
    CmCard(modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small)) {
        Text(stringResource(R.string.rules_battery_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.rules_battery_body), style = MaterialTheme.typography.bodySmall)
        CmButton(
            text = stringResource(R.string.rules_battery_action),
            onClick = onRequest,
            modifier = Modifier.padding(top = spacing.small),
        )
    }
}
