package com.sforca.callmanager.feature.home

import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.feature.onboarding.OnboardingStep
import com.sforca.callmanager.feature.onboarding.PermissionsState

data class HomeUiState(
    val permissions: PermissionsState = PermissionsState(),
    val calls: List<RecentCall> = emptyList(),
    val blockedCalls: List<UnifiedBlockedCall> = emptyList(),
)

internal class HomeActions(
    val onFix: (OnboardingStep) -> Unit,
    val onCallSelected: (number: String) -> Unit,
    val onSeeAll: () -> Unit,
)
