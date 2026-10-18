package com.matheus.callmanager.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.matheus.callmanager.core.designsystem.component.CmTextButton
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing

/** Persistent warning shown when screening can't work because the role or a permission is missing. */
@Composable
fun MissingPermissionsBanner(
    state: PermissionsState,
    onFix: (OnboardingStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Battery exemption is optional, so only the core steps warrant a persistent warning.
    if (!state.showBanner) return
    val step = state.nextStep ?: return
    val spacing = LocalSpacing.current
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                if (step == OnboardingStep.ROLE) {
                    "Call Manager isn't your call-screening app, so calls aren't being screened."
                } else {
                    "Call Manager is missing permissions it needs to work."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            CmTextButton(text = "Fix", onClick = { onFix(step) })
        }
    }
}
