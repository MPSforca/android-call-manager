package com.matheus.callmanager.feature.recent

import android.provider.CallLog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matheus.callmanager.core.data.BlockedCallMerger
import com.matheus.callmanager.core.model.RecentCallGroup
import com.matheus.callmanager.core.model.UnifiedBlockedCall
import com.matheus.callmanager.core.telephony.CallLogUtil
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter
import java.text.DateFormat
import java.util.Date

/**
 * Navigation destination for one number's call history. Owns its own [RecentViewModel] (scoped to
 * the back-stack entry) and loads its data on entry, so it also works when opened directly from a
 * deep link without the Recent tab beneath it.
 */
@Composable
fun CallDetailRoute(
    number: String,
    onBack: () -> Unit,
    viewModel: RecentViewModel = hiltViewModel(),
) {
    LaunchedEffect(number) {
        viewModel.refreshRecentCalls()
        viewModel.refreshBlockedCalls()
    }
    val calls by viewModel.recentCalls.collectAsState()
    val blockList by viewModel.blockList.collectAsState()
    val allowList by viewModel.allowList.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    val context = LocalContext.current
    val defaultRegion = remember(context) { PhoneNumberFormatter.defaultRegion(context) }
    val group =
        remember(calls, number) {
            CallLogUtil.groupByNumber(calls).find { it.number == number } ?: RecentCallGroup(number, null, emptyList())
        }
    CallDetailScreen(
        viewModel = viewModel,
        group = group,
        status = statusOf(number, blockList.map { it.number }.toSet(), allowList.map { it.number }.toSet()),
        blockedCalls = blockedCalls,
        defaultRegion = defaultRegion,
        onBack = onBack,
    )
}

@Composable
internal fun CallDetailScreen(
    viewModel: RecentViewModel,
    group: RecentCallGroup,
    status: NumberStatus,
    blockedCalls: List<UnifiedBlockedCall>,
    defaultRegion: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val formattedNumber =
        remember(group.number, defaultRegion) {
            PhoneNumberFormatter.format(context, group.number, defaultRegion)
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.name ?: formattedNumber, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(status, modifier = Modifier.padding(start = 8.dp))
                }
                if (group.name != null) {
                    Text(formattedNumber, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.toggleAllow(group.number, group.name) },
            ) {
                Text(if (status == NumberStatus.ALLOWED) "Remove from allow list" else "Add to allow list")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.toggleBlock(group.number, group.name) },
            ) {
                Text(if (status == NumberStatus.BLOCKED) "Unblock" else "Block")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Call history",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn {
            items(group.calls, key = { it.timestamp }) { call ->
                val wasBlocked = BlockedCallMerger.wasBlocked(call.number, call.timestamp, blockedCalls)
                ListItem(
                    leadingContent = {
                        Icon(
                            if (wasBlocked) Icons.Filled.Block else callTypeIcon(call.type),
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(DateFormat.getDateTimeInstance().format(Date(call.timestamp))) },
                    supportingContent = { Text(callOutcomeLabel(call, blockedCalls)) },
                )
                HorizontalDivider()
            }
        }
    }
}

private fun callTypeIcon(type: Int) =
    when (type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        CallLog.Calls.VOICEMAIL_TYPE -> Icons.Filled.Voicemail
        CallLog.Calls.REJECTED_TYPE -> Icons.Filled.PhoneDisabled
        CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> Icons.AutoMirrored.Filled.PhoneForwarded
        else -> Icons.Filled.Call
    }
