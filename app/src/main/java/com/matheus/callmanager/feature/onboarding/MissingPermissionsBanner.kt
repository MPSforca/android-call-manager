package com.matheus.callmanager.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.matheus.callmanager.core.designsystem.component.PermissionBanner

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
    PermissionBanner(
        message =
            if (step == OnboardingStep.ROLE) {
                "Call Manager isn't your call-screening app, so calls aren't being screened."
            } else {
                "Call Manager is missing permissions it needs to work."
            },
        actionLabel = "Fix",
        onAction = { onFix(step) },
        modifier = modifier,
    )
}
