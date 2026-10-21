package com.sforca.callmanager.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.model.BlockedCallAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

        val uiState: StateFlow<RulesUiState> =
            combine(
                settings.preferences,
                repository.observeBlockList(),
                repository.observeAllowList(),
                repository.observeAllowedNamePatterns(),
            ) { prefs, blocked, allowed, patterns ->
                RulesUiState(prefs, ListCounts(blocked.size, allowed.size, patterns.size))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

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
