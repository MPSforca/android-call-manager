package com.matheus.callmanager.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel
    @Inject
    constructor(
        private val repository: CallRepository,
        private val pager: SystemBlockedCallsPager,
    ) : ViewModel() {
        val blockList: StateFlow<List<BlockListEntry>> =
            repository.observeBlockList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val allowList: StateFlow<List<AllowListEntry>> =
            repository.observeAllowList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val allowedNamePatterns: StateFlow<List<AllowedNamePattern>> =
            repository
                .observeAllowedNamePatterns()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val blockedCalls: StateFlow<List<UnifiedBlockedCall>> = pager.blockedCalls
        val hasMoreBlockedCalls: StateFlow<Boolean> = pager.hasMoreBlockedCalls

        fun refreshBlockedCalls() = pager.refresh()

        fun loadMoreBlockedCalls() = pager.loadMore()

        fun blockContact(
            number: String,
            name: String?,
        ) {
            viewModelScope.launch { repository.blockNumbers(listOf(number to name)) }
        }

        fun unblock(entry: BlockListEntry) {
            viewModelScope.launch { repository.unblock(entry) }
        }

        fun allowContact(
            number: String,
            name: String?,
        ) {
            viewModelScope.launch { repository.allowNumbers(listOf(number to name)) }
        }

        fun unallow(entry: AllowListEntry) {
            viewModelScope.launch { repository.unallow(entry) }
        }

        fun addAllowedNamePattern(pattern: String) {
            if (pattern.isBlank()) return
            viewModelScope.launch { repository.addAllowedNamePattern(pattern) }
        }

        fun removeAllowedNamePattern(entry: AllowedNamePattern) {
            viewModelScope.launch { repository.removeAllowedNamePattern(entry) }
        }
    }
