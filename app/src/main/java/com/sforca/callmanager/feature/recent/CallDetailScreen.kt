package com.sforca.callmanager.feature.recent

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sforca.callmanager.R
import com.sforca.callmanager.core.data.BlockedCallMerger
import com.sforca.callmanager.core.designsystem.component.AvatarInitials
import com.sforca.callmanager.core.designsystem.component.CmButton
import com.sforca.callmanager.core.designsystem.component.CmOutlinedButton
import com.sforca.callmanager.core.designsystem.component.EmptyState
import com.sforca.callmanager.core.designsystem.component.SectionHeader
import com.sforca.callmanager.core.designsystem.component.StatusPill
import com.sforca.callmanager.core.designsystem.component.StatusTone
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.RecentCallGroup
import com.sforca.callmanager.core.model.UnifiedBlockedCall
import com.sforca.callmanager.core.telephony.CallLogUtil
import com.sforca.callmanager.core.telephony.PhoneNumberFormatter
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
    CallDetailContent(
        group = group,
        status = status,
        blockedCalls = blockedCalls,
        defaultRegion = defaultRegion,
        onBack = onBack,
        onToggleAllow = { viewModel.toggleAllow(group.number, group.name) },
        onToggleBlock = { viewModel.toggleBlock(group.number, group.name) },
    )
}

/** Stateless body of [CallDetailScreen]: the number's header, its block/allow actions and history. */
@Composable
internal fun CallDetailContent(
    group: RecentCallGroup,
    status: NumberStatus,
    blockedCalls: List<UnifiedBlockedCall>,
    defaultRegion: String,
    onBack: () -> Unit,
    onToggleAllow: () -> Unit,
    onToggleBlock: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val formattedNumber =
        remember(group.number, defaultRegion) {
            PhoneNumberFormatter.format(context, group.number, defaultRegion)
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        DetailHeader(group.name, formattedNumber, status, onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            CmOutlinedButton(
                text =
                    stringResource(
                        if (status ==
                            NumberStatus.ALLOWED
                        ) {
                            R.string.call_detail_allow_remove
                        } else {
                            R.string.call_detail_allow_add
                        },
                    ),
                onClick = onToggleAllow,
                modifier = Modifier.weight(1f),
            )
            CmButton(
                text =
                    stringResource(
                        if (status ==
                            NumberStatus.BLOCKED
                        ) {
                            R.string.call_detail_unblock
                        } else {
                            R.string.call_detail_block
                        },
                    ),
                onClick = onToggleBlock,
                modifier = Modifier.weight(1f),
            )
        }
        SectionHeader(title = stringResource(R.string.call_detail_history))
        if (group.calls.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.History,
                title = stringResource(R.string.call_detail_empty_title),
                message = stringResource(R.string.call_detail_empty_message),
            )
        } else {
            LazyColumn {
                items(group.calls, key = { it.timestamp }) { call ->
                    HistoryRow(call, blockedCalls)
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    name: String?,
    formattedNumber: String,
    status: NumberStatus,
    onBack: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.small, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.call_detail_back))
        }
        AvatarInitials(name = name ?: formattedNumber, size = 48.dp)
        Column(modifier = Modifier.weight(1f).padding(horizontal = spacing.medium)) {
            Text(name ?: formattedNumber, style = MaterialTheme.typography.titleLarge)
            if (name != null) {
                Text(
                    formattedNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (status) {
            NumberStatus.BLOCKED ->
                StatusPill(stringResource(R.string.call_detail_status_blocked), StatusTone.BLOCKED)
            NumberStatus.ALLOWED ->
                StatusPill(stringResource(R.string.call_detail_status_allowed), StatusTone.ALLOWED)
            NumberStatus.NONE -> Unit
        }
    }
}

@Composable
private fun HistoryRow(
    call: RecentCall,
    blockedCalls: List<UnifiedBlockedCall>,
) {
    val spacing = LocalSpacing.current
    val wasBlocked = BlockedCallMerger.wasBlocked(call.number, call.timestamp, blockedCalls)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (wasBlocked) Icons.Filled.Block else callTypeIcon(call.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            DateFormat.getDateTimeInstance().format(Date(call.timestamp)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(horizontal = spacing.medium),
        )
        StatusPill(
            text = callOutcomeLabel(call, blockedCalls),
            tone = if (wasBlocked) StatusTone.BLOCKED else StatusTone.UNKNOWN,
        )
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
