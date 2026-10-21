package com.sforca.callmanager.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class OnboardingStep { ROLE, PERMISSIONS, BATTERY }

data class PermissionsState(
    val hasRole: Boolean = false,
    val hasPermissions: Boolean = false,
    val batteryExempt: Boolean = false,
    /** The user chose "Skip for now" on the first-run flow. */
    val skipped: Boolean = false,
) {
    /** What the app can't work without: the screening role and the runtime permissions. */
    val coreComplete: Boolean get() = hasRole && hasPermissions

    fun isDone(step: OnboardingStep): Boolean =
        when (step) {
            OnboardingStep.ROLE -> hasRole
            OnboardingStep.PERMISSIONS -> hasPermissions
            OnboardingStep.BATTERY -> batteryExempt
        }

    /** First step still outstanding, or null when every step is satisfied. */
    val nextStep: OnboardingStep?
        get() =
            when {
                !hasRole -> OnboardingStep.ROLE
                !hasPermissions -> OnboardingStep.PERMISSIONS
                !batteryExempt -> OnboardingStep.BATTERY
                else -> null
            }

    /** The first-run flow stays up until the core steps are done or the user skips it. */
    val needsOnboarding: Boolean get() = !coreComplete && !skipped

    /** Something core is missing, e.g. onboarding was skipped or a permission was later revoked. */
    val showBanner: Boolean get() = !coreComplete
}

@HiltViewModel
class PermissionsViewModel
    @Inject
    constructor(
        private val status: SystemPermissionStatus,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val _state = MutableStateFlow(read())
        val state: StateFlow<PermissionsState> = _state.asStateFlow()

        /** Re-reads system state; call after returning from a system dialog or on resume. */
        fun refresh() {
            _state.value = read()
        }

        fun skip() {
            savedState[KEY_SKIPPED] = true
            refresh()
        }

        private fun read() =
            PermissionsState(
                hasRole = status.hasScreeningRole(),
                hasPermissions = status.hasRuntimePermissions(),
                batteryExempt = status.isIgnoringBatteryOptimizations(),
                skipped = savedState[KEY_SKIPPED] ?: false,
            )

        private companion object {
            const val KEY_SKIPPED = "onboardingSkipped"
        }
    }
