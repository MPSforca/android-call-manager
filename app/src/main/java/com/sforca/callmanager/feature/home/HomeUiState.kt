package com.sforca.callmanager.feature.home

import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.feature.onboarding.OnboardingStep
import com.sforca.callmanager.feature.onboarding.PermissionsState

data class HomeUiState(
    val permissions: PermissionsState = PermissionsState(),
    val calls: List<RecentCall> = emptyList(),
    val blockedCalls: List<UnifiedBlockedCall> = emptyList(),
    /** Show the one-time note that more rules live in the Rules tab (right after first-run setup). */
    val showRulesHint: Boolean = false,
)

internal class HomeActions(
    val onFix: (OnboardingStep) -> Unit,
    val onCallSelected: (number: String) -> Unit,
    val onSeeAll: () -> Unit,
    val onDismissRulesHint: () -> Unit = {},
    val onOpenRules: () -> Unit = {},
)

/** Shown once after first-run setup: points to the Rules tab; [onDismiss] hides it, [onOpen] goes there. */
class RulesHint(
    val onDismiss: () -> Unit,
    val onOpen: () -> Unit,
)
