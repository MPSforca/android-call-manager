package com.matheus.callmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.data.AllowListEntry
import com.matheus.callmanager.data.BlockListEntry
import com.matheus.callmanager.data.BlockedCallMerger
import com.matheus.callmanager.data.CallRepository
import com.matheus.callmanager.data.ScreeningDecision
import com.matheus.callmanager.data.UnifiedBlockedCall
import com.matheus.callmanager.util.CallLogUtil
import com.matheus.callmanager.util.RecentCall
import com.matheus.callmanager.util.SystemBlockedCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CallRepository.get(application)
    val settings get() = repository.settings

    private val _recentCalls = MutableStateFlow<List<RecentCall>>(emptyList())
    val recentCalls: StateFlow<List<RecentCall>> = _recentCalls.asStateFlow()
    private val _hasMoreRecentCalls = MutableStateFlow(true)
    val hasMoreRecentCalls: StateFlow<Boolean> = _hasMoreRecentCalls.asStateFlow()

    private val _systemBlockedCalls = MutableStateFlow<List<SystemBlockedCall>>(emptyList())
    private val _hasMoreBlockedCalls = MutableStateFlow(true)
    val hasMoreBlockedCalls: StateFlow<Boolean> = _hasMoreBlockedCalls.asStateFlow()

    val blockList: StateFlow<List<BlockListEntry>> = stateFlowFrom(repository.observeBlockList(), emptyList())
    val allowList: StateFlow<List<AllowListEntry>> = stateFlowFrom(repository.observeAllowList(), emptyList())

    /**
     * Merges this app's own block log with every call the system call log tagged BLOCKED_TYPE —
     * covering Android's native "Blocked numbers" list, the carrier, and other call-screening apps —
     * so the Blocked tab isn't limited to calls this app happened to be running for.
     */
    val blockedCalls: StateFlow<List<UnifiedBlockedCall>> = run {
        val state = MutableStateFlow<List<UnifiedBlockedCall>>(emptyList())
        viewModelScope.launch {
            combine(repository.observeBlockedCallLog(), _systemBlockedCalls) { ownLog, systemLog ->
                BlockedCallMerger.merge(ownLog, systemLog)
            }.collect { state.value = it }
        }
        state.asStateFlow()
    }

    private val _simulationResult = MutableStateFlow<String?>(null)
    val simulationResult: StateFlow<String?> = _simulationResult.asStateFlow()

    fun refreshRecentCalls() {
        val page = CallLogUtil.recentCalls(getApplication(), offset = 0)
        _recentCalls.value = page
        _hasMoreRecentCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
    }

    /** Appends the next page of recent calls, if there is one. */
    fun loadMoreRecentCalls() {
        if (!_hasMoreRecentCalls.value) return
        val page = CallLogUtil.recentCalls(getApplication(), offset = _recentCalls.value.size)
        _recentCalls.value = _recentCalls.value + page
        _hasMoreRecentCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
    }

    fun refreshSystemBlockedCalls() {
        val page = CallLogUtil.systemBlockedCalls(getApplication(), offset = 0)
        _systemBlockedCalls.value = page
        _hasMoreBlockedCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
    }

    /** Appends the next page of system-blocked calls, if there is one. */
    fun loadMoreBlockedCalls() {
        if (!_hasMoreBlockedCalls.value) return
        val page = CallLogUtil.systemBlockedCalls(getApplication(), offset = _systemBlockedCalls.value.size)
        _systemBlockedCalls.value = _systemBlockedCalls.value + page
        _hasMoreBlockedCalls.value = page.size == CallLogUtil.DEFAULT_PAGE_SIZE
    }

    fun blockNumbers(numbers: Set<String>) {
        if (numbers.isEmpty()) return
        viewModelScope.launch { repository.blockNumbers(numbers.map { it to null }) }
    }

    fun allowNumbers(numbers: Set<String>) {
        if (numbers.isEmpty()) return
        viewModelScope.launch { repository.allowNumbers(numbers.map { it to null }) }
    }

    fun blockContact(number: String, name: String?) {
        viewModelScope.launch { repository.blockNumbers(listOf(number to name)) }
    }

    fun unblock(entry: BlockListEntry) {
        viewModelScope.launch { repository.unblock(entry) }
    }

    fun allowContact(number: String, name: String?) {
        viewModelScope.launch { repository.allowNumbers(listOf(number to name)) }
    }

    fun unallow(entry: AllowListEntry) {
        viewModelScope.launch { repository.unallow(entry) }
    }

    /** Toggles a single number's block-list membership (used by the call-detail screen). */
    fun toggleBlock(number: String, name: String?) {
        viewModelScope.launch {
            val existing = blockList.value.firstOrNull { it.number == number }
            if (existing != null) repository.unblock(existing) else repository.blockNumbers(listOf(number to name))
        }
    }

    /** Toggles a single number's allow-list membership (used by the call-detail screen). */
    fun toggleAllow(number: String, name: String?) {
        viewModelScope.launch {
            val existing = allowList.value.firstOrNull { it.number == number }
            if (existing != null) repository.unallow(existing) else repository.allowNumbers(listOf(number to name))
        }
    }

    /** Runs the same rule evaluation the real CallScreeningService would, without needing an actual call. */
    fun simulateIncomingCall(number: String) {
        viewModelScope.launch {
            val decision = repository.evaluate(number)
            _simulationResult.value = when (decision) {
                is ScreeningDecision.Allow -> "ALLOWED — ${decision.reason}"
                is ScreeningDecision.Reject -> {
                    repository.logBlockedCall(number, decision.reason)
                    "REJECTED — ${decision.reason}"
                }
            }
        }
    }

    fun clearSimulationResult() {
        _simulationResult.value = null
    }

    private fun <T> stateFlowFrom(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch { flow.collect { state.value = it } }
        return state.asStateFlow()
    }
}
