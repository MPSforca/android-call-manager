package com.matheus.callmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.data.BlockListEntry
import com.matheus.callmanager.data.BlockedCallLog
import com.matheus.callmanager.data.CallRepository
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
    val blockedCallLog: StateFlow<List<BlockedCallLog>> = stateFlowFrom(repository.observeBlockedCallLog(), emptyList())

    fun refreshRecentCalls() {
        _recentCalls.value = CallLogUtil.recentCalls(getApplication())
    }

    fun blockNumbers(numbers: Set<String>) {
        if (numbers.isEmpty()) return
        viewModelScope.launch { repository.blockNumbers(numbers.toList()) }
    }

    fun unblock(entry: BlockListEntry) {
        viewModelScope.launch { repository.unblock(entry) }
    }

    private fun <T> stateFlowFrom(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch { flow.collect { state.value = it } }
        return state.asStateFlow()
    }
}
