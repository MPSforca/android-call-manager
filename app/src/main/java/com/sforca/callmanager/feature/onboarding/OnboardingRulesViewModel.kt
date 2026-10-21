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

        /**
         * Calls [onResult] with true when the user has never completed the first-run rules page. In that
         * case the recommended choices (reject unknown numbers, reject blocked calls) are saved first,
         * so the page opens showing exactly what is stored. This runs once per transition, so it never
         * overwrites a choice the user makes on the page itself.
         */
        fun needsRulesSetup(onResult: (Boolean) -> Unit) {
            viewModelScope.launch {
                val needed = !settings.preferences.first().rulesSetupCompleted
                if (needed) {
                    settings.setBlockUnknownNumbers(true)
                    settings.setBlockedCallAction(BlockedCallAction.REJECT)
                }
                onResult(needed)
            }
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
