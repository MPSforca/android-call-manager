package com.matheus.callmanager.feature.rules.steps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.data.rule.ScreeningRuleId
import com.matheus.callmanager.core.designsystem.component.CmSliderRow
import com.matheus.callmanager.core.designsystem.component.CmSwitchRow
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.feature.rules.RulesStep
import com.matheus.callmanager.feature.rules.RulesViewModel

private val DAYS_RANGE = 1..90

/** Step 2: allow rules for callers the user already has a relationship with. */
@Composable
internal fun PeopleYouKnowStep(
    prefs: UserPreferences,
    viewModel: RulesViewModel,
    onInfo: (ScreeningRuleId) -> Unit,
) {
    val contacts = ruleInfo(ScreeningRuleId.CONTACT, onInfo)
    val accepted = ruleInfo(ScreeningRuleId.RECENTLY_ACCEPTED, onInfo)
    val called = ruleInfo(ScreeningRuleId.RECENTLY_CALLED, onInfo)
    StepHeader(RulesStep.PEOPLE_YOU_KNOW)
    CmSwitchRow(
        title = stringResource(R.string.rules_contacts_title),
        subtitle = stringResource(R.string.rules_contacts_subtitle),
        checked = prefs.allowContacts,
        onCheckedChange = { viewModel.setAllowContacts(it) },
        onInfoClick = contacts.onClick,
        infoContentDescription = contacts.contentDescription,
    )
    CmSwitchRow(
        title = stringResource(R.string.rules_recent_accepted_title),
        subtitle = stringResource(R.string.rules_recent_accepted_subtitle),
        checked = prefs.recentAcceptedRuleEnabled,
        onCheckedChange = { viewModel.setRecentAcceptedRuleEnabled(it) },
        onInfoClick = accepted.onClick,
        infoContentDescription = accepted.contentDescription,
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
        onInfoClick = called.onClick,
        infoContentDescription = called.contentDescription,
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
