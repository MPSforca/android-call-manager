package com.matheus.callmanager.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.feature.onboarding.OnboardingStep
import com.matheus.callmanager.feature.onboarding.PermissionsState
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    permissions: PermissionsState,
    onFix: (OnboardingStep) -> Unit,
    onCallSelected: (number: String) -> Unit,
    onSeeAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val blockedCalls by viewModel.blockedCalls.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    HomeContent(
        state = HomeUiState(permissions = permissions, calls = calls, blockedCalls = blockedCalls),
        today = LocalDate.now(zone),
        zone = zone,
        actions = HomeActions(onFix = onFix, onCallSelected = onCallSelected, onSeeAll = onSeeAll),
    )
}
