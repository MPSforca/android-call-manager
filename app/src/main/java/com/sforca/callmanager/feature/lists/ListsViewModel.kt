package com.sforca.callmanager.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.database.entity.AllowListEntry
import com.sforca.callmanager.core.database.entity.AllowedNamePattern
import com.sforca.callmanager.core.database.entity.BlockListEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel
    @Inject
    constructor(
        private val repository: CallRepository,
    ) : ViewModel() {
        private data class Selection(
            val tab: ListTab = ListTab.Blocked,
            val query: String = "",
        )

        // One flow so switching tab and clearing the query is a single emission.
        private val selection = MutableStateFlow(Selection())

        val uiState: StateFlow<ListsUiState> =
            combine(
                selection,
                repository.observeBlockList(),
                repository.observeAllowList(),
                repository.observeAllowedNamePatterns(),
            ) { (tab, query), blocked, allowed, patterns ->
                ListsUiState(
                    tab = tab,
                    query = query,
                    blocked = blocked.filter { it.matches(query) },
                    allowed = allowed.filter { it.matches(query) },
                    namePatterns = patterns.filter { it.pattern.contains(query.trim(), ignoreCase = true) },
                    totalInTab =
                        when (tab) {
                            ListTab.Blocked -> blocked.size
                            ListTab.Allowed -> allowed.size
                            ListTab.NamePatterns -> patterns.size
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListsUiState())

        fun selectTab(value: ListTab) {
            selection.value = Selection(tab = value)
        }

        fun setQuery(value: String) {
            selection.update { it.copy(query = value) }
        }

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

private fun BlockListEntry.matches(query: String) = matchesQuery(query, number, displayName)

private fun AllowListEntry.matches(query: String) = matchesQuery(query, number, displayName)

private fun matchesQuery(
    query: String,
    number: String,
    name: String?,
): Boolean {
    val q = query.trim()
    return q.isEmpty() || number.contains(q) || name?.contains(q, ignoreCase = true) == true
}
