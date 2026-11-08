package com.sforca.callmanager.feature.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.component.CallRow
import com.sforca.callmanager.core.designsystem.component.CmOutlinedButton
import com.sforca.callmanager.core.designsystem.component.EmptyState
import com.sforca.callmanager.core.designsystem.component.SectionHeader
import com.sforca.callmanager.core.designsystem.component.StatusTone
import com.sforca.callmanager.core.designsystem.component.SwipeToDismissRow
import com.sforca.callmanager.core.designsystem.component.showUndo
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.core.model.RecentCallGroup
import com.sforca.callmanager.core.telephony.PhoneNumberFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Stateless body of [RecentScreen]: a search field, the filter chips, and the calls grouped under
 * a header per day, with one row per number inside each day. [today] and [zone] are parameters so
 * the day headers are deterministic under test.
 */
@Composable
internal fun RecentContent(
    state: RecentUiState,
    today: LocalDate,
    zone: ZoneId,
    actions: RecentActions,
) {
    val spacing = LocalSpacing.current
    val days = remember(state.calls, zone) { state.calls.groupByDay(zone) }
    val snackbarHost = remember { SnackbarHostState() }
    Box {
        Column {
            SearchField(query = state.query, onQueryChange = actions.onQueryChange)
            FilterChips(selected = state.filter, onSelected = actions.onFilterSelected)
            if (days.isEmpty()) {
                EmptyRecent(isFiltering = state.isFiltering, onClearFilters = actions.onClearFilters)
            } else {
                LazyColumn(
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(bottom = spacing.large),
                ) {
                    days.forEach { day ->
                        item(key = "header-${day.date}") { SectionHeader(title = dayTitle(day.date, today)) }
                        items(day.groups, key = { "${day.date}-${it.number}" }) { group ->
                            RecentGroupRow(
                                group = group,
                                state = state,
                                zone = zone,
                                actions = actions,
                                snackbarHost = snackbarHost,
                            )
                        }
                    }
                    if (state.hasMore) {
                        item(key = "load-more") {
                            CmOutlinedButton(
                                text = stringResource(R.string.recent_load_more),
                                onClick = actions.onLoadMore,
                                modifier = Modifier.fillMaxWidth().padding(spacing.large),
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.recent_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.recent_search_clear))
                    }
                }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
    )
}

@Composable
private fun FilterChips(
    selected: RecentFilter,
    onSelected: (RecentFilter) -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = spacing.large),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        RecentFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(filter.labelRes())) },
            )
        }
    }
}

private fun RecentFilter.labelRes(): Int =
    when (this) {
        RecentFilter.ALL -> R.string.recent_filter_all
        RecentFilter.BLOCKED -> R.string.recent_filter_blocked
        RecentFilter.MISSED -> R.string.recent_filter_missed
        RecentFilter.ANSWERED -> R.string.recent_filter_answered
    }

@Composable
private fun EmptyRecent(
    isFiltering: Boolean,
    onClearFilters: () -> Unit,
) {
    if (isFiltering) {
        EmptyState(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.recent_no_match_title),
            message = stringResource(R.string.recent_no_match_message),
            actionLabel = stringResource(R.string.recent_clear_filters),
            onAction = onClearFilters,
        )
    } else {
        EmptyState(
            icon = Icons.Filled.History,
            title = stringResource(R.string.recent_empty_title),
            message = stringResource(R.string.recent_empty_message),
        )
    }
}

@Composable
private fun RecentGroupRow(
    group: RecentCallGroup,
    state: RecentUiState,
    zone: ZoneId,
    actions: RecentActions,
    snackbarHost: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formattedNumber =
        remember(group.number) {
            PhoneNumberFormatter.format(context, group.number, PhoneNumberFormatter.defaultRegion(context))
        }
    val count =
        if (group.calls.size > 1) {
            pluralStringResource(R.plurals.recent_call_count, group.calls.size, group.calls.size)
        } else {
            null
        }
    val displayName = group.name ?: formattedNumber
    val blockedMessage = stringResource(R.string.recent_blocked_snackbar, displayName)
    val undoLabel = stringResource(R.string.recent_undo)
    val row: @Composable () -> Unit = {
        RecentCallRow(group, displayName, formattedNumber, count, state, zone) {
            actions.onCallSelected(group.number)
        }
    }
    if (group.number in state.blockedNumbers) {
        row()
    } else {
        val restoreAllow = group.number in state.allowedNumbers
        SwipeToDismissRow(
            onDismiss = {
                actions.onBlock(group.number, group.name)
                scope.launch {
                    snackbarHost.showUndo(blockedMessage, undoLabel) {
                        actions.onUndoBlock(group.number, group.name, restoreAllow)
                    }
                }
            },
            icon = Icons.Filled.Block,
            deleteContentDescription = stringResource(R.string.recent_swipe_block),
            content = row,
        )
    }
}

@Composable
private fun RecentCallRow(
    group: RecentCallGroup,
    displayName: String,
    formattedNumber: String,
    count: String?,
    state: RecentUiState,
    zone: ZoneId,
    onClick: () -> Unit,
) {
    val latest = group.calls.first()
    val blocked = latest.isBlocked(state.blockedCalls)
    CallRow(
        displayName = displayName,
        number = listOfNotNull(formattedNumber.takeIf { group.name != null }, count).joinToString(" · "),
        timeLabel = remember(latest.timestamp, zone) { formatTime(latest.timestamp, zone) },
        outcomeLabel = stringResource(callOutcomeLabel(latest, state.blockedCalls)),
        outcomeTone =
            when {
                blocked || group.number in state.blockedNumbers -> StatusTone.BLOCKED
                group.number in state.allowedNumbers -> StatusTone.ALLOWED
                else -> StatusTone.UNKNOWN
            },
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = LocalSpacing.current.large),
    )
}

@Composable
private fun dayTitle(
    date: LocalDate,
    today: LocalDate,
): String =
    when (date) {
        today -> stringResource(R.string.recent_day_today)
        today.minusDays(1) -> stringResource(R.string.recent_day_yesterday)
        else -> formatDayHeader(date)
    }

internal fun formatDayHeader(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)

private fun formatTime(
    timestamp: Long,
    zone: ZoneId,
): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(Instant.ofEpochMilli(timestamp))
