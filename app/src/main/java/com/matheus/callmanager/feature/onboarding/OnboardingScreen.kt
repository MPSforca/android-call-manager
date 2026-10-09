package com.matheus.callmanager.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private class StepContent(
    val title: String,
    val why: String,
    val action: String,
)

private fun contentFor(step: OnboardingStep) =
    when (step) {
        OnboardingStep.ROLE ->
            StepContent(
                title = "Make Call Manager your call-screening app",
                why =
                    "Android only lets one app screen incoming calls. Taking that role is what lets " +
                        "Call Manager silence or reject calls that match your rules. It does not replace " +
                        "your phone app.",
                action = "Set as call-screening app",
            )
        OnboardingStep.PERMISSIONS ->
            StepContent(
                title = "Allow access to calls and contacts",
                why =
                    "Call log access shows your recent and blocked calls, contacts access lets " +
                        "known people through, and phone state detects calls you answer. " +
                        "Everything stays on this device.",
                action = "Grant permissions",
            )
        OnboardingStep.BATTERY ->
            StepContent(
                title = "Keep screening reliable",
                why =
                    "Some phone makers stop apps in the background, which can prevent Android from " +
                        "reaching Call Manager when a call arrives. Exempting it from battery " +
                        "optimization avoids that and does not keep anything running.",
                action = "Exempt from battery optimization",
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

    val content = contentFor(step)
    val stepNumber = step.ordinal + 1
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Step $stepNumber of ${OnboardingStep.entries.size}", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { stepNumber / OnboardingStep.entries.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(content.title, style = MaterialTheme.typography.headlineSmall)
        Text(content.why, style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick =
                when (step) {
                    OnboardingStep.ROLE -> onRequestRole
                    OnboardingStep.PERMISSIONS -> onRequestPermissions
                    OnboardingStep.BATTERY -> onRequestBatteryExemption
                },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(content.action) }
        // The battery step is optional once the core steps are done, so skipping just enters the app.
        TextButton(onClick = if (step == OnboardingStep.BATTERY) onFinished else onSkip) {
            Text(if (step == OnboardingStep.BATTERY) "Not now" else "Skip for now")
        }
    }
}
