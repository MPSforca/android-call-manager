package com.sforca.callmanager.feature.recent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sforca.callmanager.core.data.BlockedCallMerger
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogUtil
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RecentScreen(
    onCallSelected: (number: String) -> Unit,
    viewModel: RecentViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRecentCalls()
        viewModel.refreshBlockedCalls()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    RecentContent(
        state = state,
        today = LocalDate.now(zone),
        zone = zone,
        actions =
            RecentActions(
                onFilterSelected = viewModel::setFilter,
                onQueryChange = viewModel::setQuery,
                onClearFilters = viewModel::clearFilters,
                onCallSelected = onCallSelected,
                onLoadMore = viewModel::loadMoreRecentCalls,
                onBlock = viewModel::blockNumber,
                onUndoBlock = viewModel::undoBlock,
            ),
    )
}

internal fun statusOf(
    number: String,
    blockedNumbers: Set<String>,
    allowedNumbers: Set<String>,
): NumberStatus =
    when {
        blockedNumbers.contains(number) -> NumberStatus.BLOCKED
        allowedNumbers.contains(number) -> NumberStatus.ALLOWED
        else -> NumberStatus.NONE
    }

/**
 * Outcome label for one call, e.g. "Blocked" or "Missed". Prefers matching against this app's own
 * blocked-call record over the system CallLog.Calls.TYPE column: a call this app silenced (allowed
 * through, muted, per the default SILENCE policy) is never tagged BLOCKED_TYPE by the system — it
 * just looks like a normal missed/incoming call there — but it IS correctly recorded as blocked in
 * our own log, so that's the source of truth for "was this call actually blocked".
 */
internal fun callOutcomeLabel(
    call: RecentCall,
    blockedCalls: List<UnifiedBlockedCall>,
): String =
    if (BlockedCallMerger.wasBlocked(call.number, call.timestamp, blockedCalls)) {
        "Blocked"
    } else {
        CallLogUtil.callTypeLabel(call.type)
    }

@Composable
internal fun StatusBadge(
    status: NumberStatus,
    modifier: Modifier = Modifier,
) {
    val (label, color) =
        when (status) {
            NumberStatus.BLOCKED -> "Blocked" to MaterialTheme.colorScheme.error
            NumberStatus.ALLOWED -> "Allowed" to MaterialTheme.colorScheme.primary
            NumberStatus.NONE -> return
        }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}
