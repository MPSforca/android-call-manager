package com.matheus.callmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.data.AllowListEntry
import com.matheus.callmanager.data.BlockListEntry
import com.matheus.callmanager.data.BlockedCallLog
import com.matheus.callmanager.data.CallRepository
import com.matheus.callmanager.data.ScreeningDecision
import com.matheus.callmanager.util.CallLogUtil
import com.matheus.callmanager.util.RecentCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CallRepository.get(application)
    val settings get() = repository.settings

    private val _recentCalls = MutableStateFlow<List<RecentCall>>(emptyList())
    val recentCalls: StateFlow<List<RecentCall>> = _recentCalls.asStateFlow()

    val blockList: StateFlow<List<BlockListEntry>> = stateFlowFrom(repository.observeBlockList(), emptyList())
    val allowList: StateFlow<List<AllowListEntry>> = stateFlowFrom(repository.observeAllowList(), emptyList())
    val blockedCallLog: StateFlow<List<BlockedCallLog>> = stateFlowFrom(repository.observeBlockedCallLog(), emptyList())

    private val _simulationResult = MutableStateFlow<String?>(null)
    val simulationResult: StateFlow<String?> = _simulationResult.asStateFlow()

    fun refreshRecentCalls() {
        _recentCalls.value = CallLogUtil.recentCalls(getApplication())
    }

    fun blockNumbers(numbers: Set<String>) {
        if (numbers.isEmpty()) return
        viewModelScope.launch { repository.blockNumbers(numbers.map { it to null }) }
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
