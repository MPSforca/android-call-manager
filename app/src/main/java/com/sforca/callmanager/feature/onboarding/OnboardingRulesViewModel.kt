package com.sforca.callmanager.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.datastore.SettingsRepository
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the first-run rules page and decides whether it still needs to be shown. */
@HiltViewModel
class OnboardingRulesViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val preferences: StateFlow<UserPreferences> =
            settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

        /** Calls [onResult] with true when the user has never completed the first-run rules page. */
        fun needsRulesSetup(onResult: (Boolean) -> Unit) {
            viewModelScope.launch { onResult(!settings.preferences.first().rulesSetupCompleted) }
        }

        fun setBlockUnknownNumbers(value: Boolean) = viewModelScope.launch { settings.setBlockUnknownNumbers(value) }

        fun setBlockedCallAction(value: BlockedCallAction) =
            viewModelScope.launch { settings.setBlockedCallAction(value) }

        /** Marks the page done so it never shows again, then calls [onDone]. */
        fun finish(onDone: () -> Unit) {
            viewModelScope.launch {
                settings.setRulesSetupCompleted(true)
                onDone()
            }
        }
    }
