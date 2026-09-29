package com.matheus.callmanager.feature.recent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matheus.callmanager.core.data.BlockedCallMerger
import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import com.matheus.callmanager.core.telephony.CallLogUtil
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun RecentScreen(viewModel: RecentViewModel = hiltViewModel()) {
    val calls by viewModel.recentCalls.collectAsState()
    val hasMoreCalls by viewModel.hasMoreRecentCalls.collectAsState()
    val blockList by viewModel.blockList.collectAsState()
    val allowList by viewModel.allowList.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selectedNumber by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val defaultRegion = remember(context) { PhoneNumberFormatter.defaultRegion(context) }

    val groups = remember(calls) { CallLogUtil.groupByNumber(calls) }
    val blockedNumbers = remember(blockList) { blockList.map { it.number }.toSet() }
    val allowedNumbers = remember(allowList) { allowList.map { it.number }.toSet() }

    val detailGroup = groups.find { it.number == selectedNumber }
    if (detailGroup != null) {
        BackHandler { selectedNumber = null }
        CallDetailScreen(
            viewModel = viewModel,
            group = detailGroup,
            status = statusOf(detailGroup.number, blockedNumbers, allowedNumbers),
            blockedCalls = blockedCalls,
            defaultRegion = defaultRegion,
            onBack = { selectedNumber = null },
        )
        return
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Tap a number for details, or select to block/allow in bulk",
                modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = {
                        viewModel.allowNumbers(selected)
                        selected = emptySet()
                    }) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Allow selected")
                    }
                    FloatingActionButton(onClick = {
                        viewModel.blockNumbers(selected)
                        selected = emptySet()
                    }) {
                        Icon(Icons.Filled.Block, contentDescription = "Block selected")
                    }
                }
            }
        }
        LazyColumn {
            items(groups, key = { it.number }) { group ->
                val status = statusOf(group.number, blockedNumbers, allowedNumbers)
                val latestCall = group.calls.first()
                val formattedNumber =
                    remember(group.number, defaultRegion) {
                        PhoneNumberFormatter.format(context, group.number, defaultRegion)
                    }
                val countSuffix = if (group.calls.size > 1) " · ${group.calls.size} calls" else ""
                ListItem(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedNumber = group.number },
                    leadingContent = {
                        Checkbox(
                            checked = selected.contains(group.number),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + group.number else selected - group.number
                            },
                        )
                    },
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                group.name ?: formattedNumber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            StatusBadge(status, modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    supportingContent = {
                        Column {
                            if (group.name != null) {
                                Text(formattedNumber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                "${DateFormat.getDateTimeInstance().format(Date(group.latestTimestamp))} · " +
                                    callOutcomeLabel(latestCall, blockedCalls) + countSuffix,
                            )
                        }
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor =
                                when (status) {
                                    NumberStatus.BLOCKED -> MaterialTheme.colorScheme.errorContainer
                                    NumberStatus.ALLOWED -> MaterialTheme.colorScheme.primaryContainer
                                    NumberStatus.NONE -> MaterialTheme.colorScheme.surface
                                },
                        ),
                )
                HorizontalDivider()
            }
            if (hasMoreCalls) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.loadMoreRecentCalls() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("Load more") }
                }
            }
        }
    }
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
