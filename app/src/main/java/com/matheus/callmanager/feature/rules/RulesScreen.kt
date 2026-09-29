package com.matheus.callmanager.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matheus.callmanager.core.model.BlockedCallAction

@Composable
fun RulesScreen(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val settings = viewModel.settings
    var allowContacts by remember { mutableStateOf(settings.allowContacts) }
    var recentEnabled by remember { mutableStateOf(settings.recentAcceptedRuleEnabled) }
    var recentDays by remember { mutableStateOf(settings.allowRecentAcceptedDays.toFloat()) }
    var outgoingEnabled by remember { mutableStateOf(settings.recentOutgoingRuleEnabled) }
    var outgoingDays by remember { mutableStateOf(settings.allowRecentOutgoingDays.toFloat()) }
    var blockUnknown by remember { mutableStateOf(settings.blockUnknownNumbers) }
    var blockedCallAction by remember { mutableStateOf(settings.blockedCallAction) }
    var showRejectedNotification by remember { mutableStateOf(settings.showRejectedCallNotification) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (!isIgnoringBatteryOptimizations) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Call screening can be silently killed", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Some phones aggressively kill background apps, which can stop calls from being " +
                        "screened at all. This app has no persistent background process — it just needs " +
                        "the system to be allowed to wake it briefly per call.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = onRequestIgnoreBatteryOptimizations,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Exempt from battery optimization")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Allow calls from contacts")
            Switch(checked = allowContacts, onCheckedChange = {
                allowContacts = it
                settings.allowContacts = it
            })
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Allow recently accepted callers")
            Switch(checked = recentEnabled, onCheckedChange = {
                recentEnabled = it
                settings.recentAcceptedRuleEnabled = it
            })
        }
        if (recentEnabled) {
            Text("Allow again for ${recentDays.toInt()} days after you answer them")
            Slider(
                value = recentDays,
                onValueChange = { recentDays = it },
                onValueChangeFinished = { settings.allowRecentAcceptedDays = recentDays.toInt() },
                valueRange = 1f..90f,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Allow recently called numbers")
            Switch(checked = outgoingEnabled, onCheckedChange = {
                outgoingEnabled = it
                settings.recentOutgoingRuleEnabled = it
            })
        }
        if (outgoingEnabled) {
            Text("Allow for ${outgoingDays.toInt()} days after you call them")
            Slider(
                value = outgoingDays,
                onValueChange = { outgoingDays = it },
                onValueChangeFinished = { settings.allowRecentOutgoingDays = outgoingDays.toInt() },
                valueRange = 1f..90f,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Reject unknown numbers by default")
            Switch(checked = blockUnknown, onCheckedChange = {
                blockUnknown = it
                settings.blockUnknownNumbers = it
            })
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("When a call is blocked", style = MaterialTheme.typography.titleSmall)
        Text(
            "Applies to your block list and, if enabled above, unknown numbers.",
            style = MaterialTheme.typography.bodySmall,
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            listOf(
                BlockedCallAction.SILENCE to "Show it silently — you can still answer, it just never rings",
                BlockedCallAction.REJECT to "Reject it outright — never surfaces as an incoming call",
            ).forEach { (action, label) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = blockedCallAction == action,
                                onClick = {
                                    blockedCallAction = action
                                    settings.blockedCallAction = action
                                },
                            ).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = blockedCallAction == action,
                        onClick = {
                            blockedCallAction = action
                            settings.blockedCallAction = action
                        },
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Notify when a call is rejected")
            Switch(checked = showRejectedNotification, onCheckedChange = {
                showRejectedNotification = it
                settings.showRejectedCallNotification = it
            })
        }
    }
}
