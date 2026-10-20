package com.sforca.callmanager.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.PermissionBanner

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
                stringResource(R.string.permission_banner_role_missing)
            } else {
                stringResource(R.string.permission_banner_permissions_missing)
            },
        actionLabel = stringResource(R.string.permission_banner_fix),
        onAction = { onFix(step) },
        modifier = modifier,
    )
}
