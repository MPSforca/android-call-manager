package com.sforca.callmanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.SystemBlockedCallsPager
import com.sforca.callmanager.core.di.IoDispatcher
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val callLogDataSource: CallLogDataSource,
        private val pager: SystemBlockedCallsPager,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _calls = MutableStateFlow<List<RecentCall>>(emptyList())

        /** The newest page of the system call log; Home only summarises it. */
        val calls: StateFlow<List<RecentCall>> = _calls.asStateFlow()
        val blockedCalls: StateFlow<List<UnifiedBlockedCall>> = pager.blockedCalls

        fun refresh() {
            pager.refresh()
            viewModelScope.launch {
                _calls.value = withContext(ioDispatcher) { callLogDataSource.recentCalls(offset = 0) }
            }
        }
    }
