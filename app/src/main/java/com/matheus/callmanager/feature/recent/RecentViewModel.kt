package com.matheus.callmanager.feature.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.data.SystemBlockedCallsPager
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.di.IoDispatcher
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import com.matheus.callmanager.core.telephony.CallLogDataSource
import com.matheus.callmanager.core.telephony.CallLogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecentViewModel
    @Inject
    constructor(
        private val repository: CallRepository,
        private val callLogDataSource: CallLogDataSource,
        private val pager: SystemBlockedCallsPager,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _recentCalls = MutableStateFlow<List<RecentCall>>(emptyList())
        val recentCalls: StateFlow<List<RecentCall>> = _recentCalls.asStateFlow()
        private val _hasMoreRecentCalls = MutableStateFlow(true)
        val hasMoreRecentCalls: StateFlow<Boolean> = _hasMoreRecentCalls.asStateFlow()

        val blockList: StateFlow<List<BlockListEntry>> =
            repository.observeBlockList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val allowList: StateFlow<List<AllowListEntry>> =
            repository.observeAllowList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val blockedCalls: StateFlow<List<UnifiedBlockedCall>> = pager.blockedCalls

        fun refreshRecentCalls() {
            viewModelScope.launch {
                val page = withContext(ioDispatcher) { callLogDataSource.recentCalls(offset = 0) }
                _recentCalls.value = page
                _hasMoreRecentCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
            }
        }

        /** Appends the next page of recent calls, if there is one. */
        fun loadMoreRecentCalls() {
            if (!_hasMoreRecentCalls.value) return
            viewModelScope.launch {
                val offset = _recentCalls.value.size
                val page = withContext(ioDispatcher) { callLogDataSource.recentCalls(offset = offset) }
                _recentCalls.value = _recentCalls.value + page
                _hasMoreRecentCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
            }
        }

        fun refreshBlockedCalls() = pager.refresh()

        fun blockNumbers(numbers: Set<String>) {
            if (numbers.isEmpty()) return
            viewModelScope.launch { repository.blockNumbers(numbers.map { it to null }) }
        }

        fun allowNumbers(numbers: Set<String>) {
            if (numbers.isEmpty()) return
            viewModelScope.launch { repository.allowNumbers(numbers.map { it to null }) }
        }

        fun unblock(entry: BlockListEntry) {
            viewModelScope.launch { repository.unblock(entry) }
        }

        fun unallow(entry: AllowListEntry) {
            viewModelScope.launch { repository.unallow(entry) }
        }

        /** Toggles a single number's block-list membership (used by the call-detail screen). */
        fun toggleBlock(
            number: String,
            name: String?,
        ) {
            viewModelScope.launch {
                val existing = blockList.value.firstOrNull { it.number == number }
                if (existing != null) repository.unblock(existing) else repository.blockNumbers(listOf(number to name))
            }
        }

        /** Toggles a single number's allow-list membership (used by the call-detail screen). */
        fun toggleAllow(
            number: String,
            name: String?,
        ) {
            viewModelScope.launch {
                val existing = allowList.value.firstOrNull { it.number == number }
                if (existing != null) repository.unallow(existing) else repository.allowNumbers(listOf(number to name))
            }
        }
    }
