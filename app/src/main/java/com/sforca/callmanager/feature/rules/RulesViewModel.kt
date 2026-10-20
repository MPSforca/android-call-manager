package com.sforca.callmanager.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.model.BlockedCallAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel
    @Inject
    constructor(
        repository: CallRepository,
    ) : ViewModel() {
        private val settings = repository.settings

        private data class PreviewInput(
            val number: String,
            val callerName: String?,
        )

        private val previewInput = MutableStateFlow<PreviewInput?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<RulesUiState> =
            combine(
                settings.preferences,
                repository.observeBlockList(),
                repository.observeAllowList(),
                repository.observeAllowedNamePatterns(),
                previewInput,
            ) { prefs, blocked, allowed, patterns, input ->
                RulesUiState(prefs, ListCounts(blocked.size, allowed.size, patterns.size)) to input
            }.mapLatest { (state, input) ->
                // Read-only: unlike Test a number, the preview never logs a blocked call.
                state.copy(
                    preview =
                        input?.let {
                            RulePreview(
                                it.number,
                                repository.evaluateWithTrace(it.number, it.callerName),
                            )
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

        /** Previews the verdict for [number] (already normalised); a blank number clears the preview. */
        fun setPreview(
            number: String,
            callerName: String? = null,
        ) {
            previewInput.value = number.takeIf { it.isNotBlank() }?.let { PreviewInput(it, callerName) }
        }

        fun setAllowContacts(value: Boolean) = viewModelScope.launch { settings.setAllowContacts(value) }

        fun setRecentAcceptedRuleEnabled(value: Boolean) =
            viewModelScope.launch {
                settings.setRecentAcceptedRuleEnabled(value)
            }

        fun setAllowRecentAcceptedDays(value: Int) =
            viewModelScope.launch { settings.setAllowRecentAcceptedDays(value) }

        fun setRecentOutgoingRuleEnabled(value: Boolean) =
            viewModelScope.launch {
                settings.setRecentOutgoingRuleEnabled(value)
            }

        fun setAllowRecentOutgoingDays(value: Int) =
            viewModelScope.launch { settings.setAllowRecentOutgoingDays(value) }

        fun setBlockUnknownNumbers(value: Boolean) = viewModelScope.launch { settings.setBlockUnknownNumbers(value) }

        fun setBlockedCallAction(value: BlockedCallAction) =
            viewModelScope.launch { settings.setBlockedCallAction(value) }

        fun setShowRejectedCallNotification(value: Boolean) =
            viewModelScope.launch { settings.setShowRejectedCallNotification(value) }
    }
