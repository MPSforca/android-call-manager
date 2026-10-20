package com.sforca.callmanager.feature.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.data.SystemBlockedCallsPager
import com.sforca.callmanager.core.database.entity.AllowListEntry
import com.sforca.callmanager.core.database.entity.BlockListEntry
import com.sforca.callmanager.core.di.IoDispatcher
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogDataSource
import com.sforca.callmanager.core.telephony.CallLogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

        private val _filter = MutableStateFlow(RecentFilter.ALL)
        val filter: StateFlow<RecentFilter> = _filter.asStateFlow()
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        /** Recent calls narrowed by the selected filter chip and the search query. */
        val visibleCalls: StateFlow<List<RecentCall>> =
            combine(_recentCalls, blockedCalls, _filter, _query) { calls, blocked, filter, query ->
                calls.filterCalls(filter, query, blocked)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val listing =
            combine(_filter, _query, visibleCalls, blockedCalls) { filter, query, calls, blocked ->
                RecentUiState(filter = filter, query = query, calls = calls, blockedCalls = blocked)
            }

        val uiState: StateFlow<RecentUiState> =
            combine(listing, blockList, allowList, _hasMoreRecentCalls) { state, blockEntries, allowEntries, hasMore ->
                state.copy(
                    blockedNumbers = blockEntries.mapTo(HashSet()) { it.number },
                    allowedNumbers = allowEntries.mapTo(HashSet()) { it.number },
                    hasMore = hasMore,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecentUiState())

        fun setFilter(filter: RecentFilter) {
            _filter.value = filter
        }

        fun setQuery(query: String) {
            _query.value = query
        }

        fun clearFilters() {
            _filter.value = RecentFilter.ALL
            _query.value = ""
        }

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

        /** Blocks one number from a swipe; pair with [undoBlock]. */
        fun blockNumber(
            number: String,
            name: String?,
        ) {
            viewModelScope.launch { repository.blockNumbers(listOf(number to name)) }
        }

        /**
         * Reverts [blockNumber]. Blocking drops any allow entry for the number, so [restoreAllow]
         * puts it back when the number had one.
         */
        fun undoBlock(
            number: String,
            name: String?,
            restoreAllow: Boolean,
        ) {
            viewModelScope.launch {
                repository.observeBlockList().first().firstOrNull { it.number == number }?.let {
                    repository.unblock(
                        it,
                    )
                }
                if (restoreAllow) repository.allowNumbers(listOf(number to name))
            }
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
