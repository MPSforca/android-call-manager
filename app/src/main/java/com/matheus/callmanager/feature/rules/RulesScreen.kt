package com.matheus.callmanager.feature.rules

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmButton
import com.matheus.callmanager.core.designsystem.component.CmCard
import com.matheus.callmanager.core.designsystem.component.CmSegmentedButton
import com.matheus.callmanager.core.designsystem.component.CmSliderRow
import com.matheus.callmanager.core.designsystem.component.CmSwitchRow
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.UserPreferences

private val DAYS_RANGE = 1..90

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
        AllowSection(prefs, viewModel)
        SectionHeader(
            title = stringResource(R.string.rules_section_block),
            subtitle = stringResource(R.string.rules_section_block_subtitle),
        )
        CmSwitchRow(
            title = stringResource(R.string.rules_unknown_title),
            subtitle = stringResource(R.string.rules_unknown_subtitle),
            checked = prefs.blockUnknownNumbers,
            onCheckedChange = { viewModel.setBlockUnknownNumbers(it) },
        )
        BlockedActionSection(prefs, viewModel)
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

@Composable
private fun AllowSection(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
) {
    SectionHeader(
        title = stringResource(R.string.rules_section_allow),
        subtitle = stringResource(R.string.rules_section_allow_subtitle),
    )
    CmSwitchRow(
        title = stringResource(R.string.rules_contacts_title),
        subtitle = stringResource(R.string.rules_contacts_subtitle),
        checked = prefs.allowContacts,
        onCheckedChange = { viewModel.setAllowContacts(it) },
    )
    CmSwitchRow(
        title = stringResource(R.string.rules_recent_accepted_title),
        subtitle = stringResource(R.string.rules_recent_accepted_subtitle),
        checked = prefs.recentAcceptedRuleEnabled,
        onCheckedChange = { viewModel.setRecentAcceptedRuleEnabled(it) },
    )
    if (prefs.recentAcceptedRuleEnabled) {
        DaysSlider(R.string.rules_recent_accepted_slider, prefs.allowRecentAcceptedDays) {
            viewModel.setAllowRecentAcceptedDays(it)
        }
    }
    CmSwitchRow(
        title = stringResource(R.string.rules_recent_outgoing_title),
        subtitle = stringResource(R.string.rules_recent_outgoing_subtitle),
        checked = prefs.recentOutgoingRuleEnabled,
        onCheckedChange = { viewModel.setRecentOutgoingRuleEnabled(it) },
    )
    if (prefs.recentOutgoingRuleEnabled) {
        DaysSlider(R.string.rules_recent_outgoing_slider, prefs.allowRecentOutgoingDays) {
            viewModel.setAllowRecentOutgoingDays(it)
        }
    }
}

@Composable
private fun DaysSlider(
    @StringRes title: Int,
    days: Int,
    onCommit: (Int) -> Unit,
) {
    val resources = LocalContext.current.resources
    CmSliderRow(
        title = stringResource(title),
        value = days,
        onValueChange = onCommit,
        valueRange = DAYS_RANGE,
        valueLabel = { resources.getQuantityString(R.plurals.rules_days, it, it) },
    )
}

@Composable
private fun BlockedActionSection(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
) {
    val spacing = LocalSpacing.current
    val labels = BlockedCallAction.entries.associateWith { stringResource(it.labelRes()) }
    SectionHeader(
        title = stringResource(R.string.rules_section_action),
        subtitle = stringResource(R.string.rules_section_action_subtitle),
    )
    CmSegmentedButton(
        options = BlockedCallAction.entries,
        selected = prefs.blockedCallAction,
        onSelect = { viewModel.setBlockedCallAction(it) },
        label = { labels.getValue(it) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
    )
    Text(
        text = stringResource(prefs.blockedCallAction.hintRes()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
    )
    CmSwitchRow(
        title = stringResource(R.string.rules_notify_title),
        subtitle = stringResource(R.string.rules_notify_subtitle),
        checked = prefs.showRejectedCallNotification,
        onCheckedChange = { viewModel.setShowRejectedCallNotification(it) },
    )
}

private fun BlockedCallAction.labelRes() =
    when (this) {
        BlockedCallAction.SILENCE -> R.string.rules_action_silence
        BlockedCallAction.REJECT -> R.string.rules_action_reject
    }

private fun BlockedCallAction.hintRes() =
    when (this) {
        BlockedCallAction.SILENCE -> R.string.rules_action_silence_hint
        BlockedCallAction.REJECT -> R.string.rules_action_reject_hint
    }
