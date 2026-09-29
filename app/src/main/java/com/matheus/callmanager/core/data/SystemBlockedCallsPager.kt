package com.matheus.callmanager.core.data

import com.matheus.callmanager.core.di.IoDispatcher
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import com.matheus.callmanager.core.telephony.CallLogDataSource
import com.matheus.callmanager.core.telephony.CallLogUtil
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Recent and Lists tabs both show the merged blocked-call log, backed by the same paged system
 * call log query. Scoping this to the activity (rather than duplicating it per-ViewModel) means
 * both tabs share one page cursor and one query, instead of each independently re-querying the
 * system call log for the same data.
 */
@ActivityRetainedScoped
class SystemBlockedCallsPager
    @Inject
    constructor(
        private val repository: CallRepository,
        private val callLogDataSource: CallLogDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
    ) {
        private val scope = CoroutineScope(SupervisorJob())

        init {
            activityRetainedLifecycle.addOnClearedListener { scope.cancel() }
        }

        private val _systemBlockedCalls = MutableStateFlow<List<SystemBlockedCall>>(emptyList())
        private val _hasMoreBlockedCalls = MutableStateFlow(true)
        val hasMoreBlockedCalls: StateFlow<Boolean> = _hasMoreBlockedCalls.asStateFlow()

        val blockedCalls: StateFlow<List<UnifiedBlockedCall>> =
            run {
                val state = MutableStateFlow<List<UnifiedBlockedCall>>(emptyList())
                scope.launch {
                    combine(repository.observeBlockedCallLog(), _systemBlockedCalls) { ownLog, systemLog ->
                        BlockedCallMerger.merge(ownLog, systemLog)
                    }.collect { state.value = it }
                }
                state.asStateFlow()
            }

        fun refresh() {
            scope.launch {
                val page = withContext(ioDispatcher) { callLogDataSource.systemBlockedCalls(offset = 0) }
                _systemBlockedCalls.value = page
                _hasMoreBlockedCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
            }
        }

        fun loadMore() {
            if (!_hasMoreBlockedCalls.value) return
            scope.launch {
                val offset = _systemBlockedCalls.value.size
                val page = withContext(ioDispatcher) { callLogDataSource.systemBlockedCalls(offset = offset) }
                _systemBlockedCalls.value = _systemBlockedCalls.value + page
                _hasMoreBlockedCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
            }
        }
    }
