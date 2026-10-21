package com.sforca.callmanager.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sforca.callmanager.feature.onboarding.OnboardingStep
import com.sforca.callmanager.feature.onboarding.PermissionsState
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    permissions: PermissionsState,
    onFix: (OnboardingStep) -> Unit,
    onCallSelected: (number: String) -> Unit,
    onSeeAll: () -> Unit,
    rulesHint: RulesHint?,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val blockedCalls by viewModel.blockedCalls.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    HomeContent(
        state =
            HomeUiState(
                permissions = permissions,
                calls = calls,
                blockedCalls = blockedCalls,
                showRulesHint = rulesHint != null,
            ),
        today = LocalDate.now(zone),
        zone = zone,
        actions =
            HomeActions(
                onFix = onFix,
                onCallSelected = onCallSelected,
                onSeeAll = onSeeAll,
                onDismissRulesHint = { rulesHint?.onDismiss?.invoke() },
                onOpenRules = { rulesHint?.onOpen?.invoke() },
            ),
    )
}
