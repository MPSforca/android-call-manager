package com.matheus.callmanager.feature.recent

import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall

/** Everything the Recent screen renders. [calls] is already narrowed by [filter] and [query]. */
data class RecentUiState(
    val filter: RecentFilter = RecentFilter.ALL,
    val query: String = "",
    val calls: List<RecentCall> = emptyList(),
    val blockedCalls: List<UnifiedBlockedCall> = emptyList(),
    val blockedNumbers: Set<String> = emptySet(),
    val allowedNumbers: Set<String> = emptySet(),
    val hasMore: Boolean = false,
) {
    val isFiltering: Boolean get() = filter != RecentFilter.ALL || query.isNotBlank()
}

/** What the user can do on the Recent screen, bundled so [RecentContent] stays readable. */
internal class RecentActions(
    val onFilterSelected: (RecentFilter) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClearFilters: () -> Unit,
    val onCallSelected: (number: String) -> Unit,
    val onLoadMore: () -> Unit,
    val onBlock: (number: String, name: String?) -> Unit,
    val onUndoBlock: (number: String, name: String?, restoreAllow: Boolean) -> Unit,
)
