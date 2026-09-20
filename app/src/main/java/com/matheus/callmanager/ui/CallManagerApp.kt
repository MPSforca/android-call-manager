package com.matheus.callmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matheus.callmanager.BuildConfig
import com.matheus.callmanager.data.BlockedCallAction
import com.matheus.callmanager.util.CallLogUtil
import com.matheus.callmanager.util.RecentCallGroup
import java.text.DateFormat
import java.util.Date

private enum class Tab(val label: String) {
    RECENT("Recent"), LISTS("Lists"), RULES("Rules"), DEBUG("Debug")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallManagerApp(
    viewModel: MainViewModel,
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.RECENT) }
    val showDebugTab = BuildConfig.DEBUG

    Scaffold(
        topBar = { TopAppBar(title = { Text("Call Manager") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.RECENT,
                    onClick = { tab = Tab.RECENT },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text(Tab.RECENT.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.LISTS,
                    onClick = { tab = Tab.LISTS },
                    icon = { Icon(Icons.Filled.Block, contentDescription = null) },
                    label = { Text(Tab.LISTS.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.RULES,
                    onClick = { tab = Tab.RULES },
                    icon = { Icon(Icons.Filled.Rule, contentDescription = null) },
                    label = { Text(Tab.RULES.label) }
                )
                if (showDebugTab) {
                    NavigationBarItem(
                        selected = tab == Tab.DEBUG,
                        onClick = { tab = Tab.DEBUG },
                        icon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                        label = { Text(Tab.DEBUG.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.RECENT -> RecentCallsScreen(viewModel)
                Tab.LISTS -> ListsScreen(viewModel, onPickContactToBlock, onPickContactToAllow)
                Tab.RULES -> RulesScreen(viewModel, isIgnoringBatteryOptimizations, onRequestIgnoreBatteryOptimizations)
                Tab.DEBUG -> DebugScreen(viewModel)
            }
        }
    }
}

private enum class NumberStatus { BLOCKED, ALLOWED, NONE }

@Composable
private fun RecentCallsScreen(viewModel: MainViewModel) {
    val calls by viewModel.recentCalls.collectAsState()
    val hasMoreCalls by viewModel.hasMoreRecentCalls.collectAsState()
    val blockList by viewModel.blockList.collectAsState()
    val allowList by viewModel.allowList.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selectedNumber by remember { mutableStateOf<String?>(null) }

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
            onBack = { selectedNumber = null }
        )
        return
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tap a number for details, or select to block/allow in bulk")
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (status) {
                                NumberStatus.BLOCKED -> MaterialTheme.colorScheme.errorContainer
                                NumberStatus.ALLOWED -> MaterialTheme.colorScheme.primaryContainer
                                NumberStatus.NONE -> MaterialTheme.colorScheme.surface
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNumber = group.number }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(group.number),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + group.number else selected - group.number
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    group.name ?: group.number,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                StatusBadge(status, modifier = Modifier.padding(start = 8.dp))
                            }
                            val countSuffix = if (group.calls.size > 1) " · ${group.calls.size} calls" else ""
                            Text(
                                "${DateFormat.getDateTimeInstance().format(Date(group.latestTimestamp))} · " +
                                    CallLogUtil.callTypeLabel(latestCall.type) + countSuffix,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
            if (hasMoreCalls) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.loadMoreRecentCalls() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("Load more") }
                }
            }
        }
    }
}

private fun statusOf(number: String, blockedNumbers: Set<String>, allowedNumbers: Set<String>): NumberStatus = when {
    blockedNumbers.contains(number) -> NumberStatus.BLOCKED
    allowedNumbers.contains(number) -> NumberStatus.ALLOWED
    else -> NumberStatus.NONE
}

@Composable
private fun StatusBadge(status: NumberStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        NumberStatus.BLOCKED -> "Blocked" to MaterialTheme.colorScheme.error
        NumberStatus.ALLOWED -> "Allowed" to MaterialTheme.colorScheme.primary
        NumberStatus.NONE -> return
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun CallDetailScreen(
    viewModel: MainViewModel,
    group: RecentCallGroup,
    status: NumberStatus,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.name ?: group.number, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(status, modifier = Modifier.padding(start = 8.dp))
                }
                if (group.name != null) {
                    Text(group.number, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.toggleAllow(group.number, group.name) }
            ) {
                Text(if (status == NumberStatus.ALLOWED) "Remove from allow list" else "Add to allow list")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.toggleBlock(group.number, group.name) }
            ) {
                Text(if (status == NumberStatus.BLOCKED) "Unblock" else "Block")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Call history",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn {
            items(group.calls, key = { it.timestamp }) { call ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(DateFormat.getDateTimeInstance().format(Date(call.timestamp)))
                    Text(CallLogUtil.callTypeLabel(call.type), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ListsScreen(
    viewModel: MainViewModel,
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit
) {
    val blockList by viewModel.blockList.collectAsState()
    val allowList by viewModel.allowList.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    val hasMoreBlockedCalls by viewModel.hasMoreBlockedCalls.collectAsState()
    var manualNumber by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            OutlinedTextField(
                value = manualNumber,
                onValueChange = { manualNumber = it },
                label = { Text("Add a number manually") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.blockContact(manualNumber, null); manualNumber = "" },
                    enabled = manualNumber.isNotBlank()
                ) { Text("Block it") }
                OutlinedButton(
                    onClick = { viewModel.allowContact(manualNumber, null); manualNumber = "" },
                    enabled = manualNumber.isNotBlank()
                ) { Text("Allow it") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Block list", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onPickContactToBlock) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact to block list")
                }
            }
        }
        items(blockList, key = { it.number }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.displayName ?: entry.number)
                IconButton(onClick = { viewModel.unblock(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow list", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onPickContactToAllow) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact to allow list")
                }
            }
        }
        items(allowList, key = { it.number }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.displayName ?: entry.number)
                IconButton(onClick = { viewModel.unallow(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Text("Blocked calls", style = MaterialTheme.typography.titleMedium)
            Text(
                "Includes calls blocked by this app, Android's own block list, and your carrier.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(blockedCalls, key = { "${it.number}-${it.timestamp}" }) { entry ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(entry.name ?: entry.number)
                Text(
                    "${entry.reason} · ${entry.source} · ${DateFormat.getDateTimeInstance().format(Date(entry.timestamp))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (hasMoreBlockedCalls) {
            item {
                OutlinedButton(
                    onClick = { viewModel.loadMoreBlockedCalls() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Load more") }
            }
        }
    }
}

@Composable
private fun RulesScreen(
    viewModel: MainViewModel,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit
) {
    val settings = viewModel.settings
    var allowContacts by remember { mutableStateOf(settings.allowContacts) }
    var recentEnabled by remember { mutableStateOf(settings.recentAcceptedRuleEnabled) }
    var recentDays by remember { mutableStateOf(settings.allowRecentAcceptedDays.toFloat()) }
    var blockUnknown by remember { mutableStateOf(settings.blockUnknownNumbers) }
    var blockedCallAction by remember { mutableStateOf(settings.blockedCallAction) }

    Column(modifier = Modifier.padding(16.dp)) {
        if (!isIgnoringBatteryOptimizations) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Call screening can be silently killed", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Some phones aggressively kill background apps, which can stop calls from being " +
                        "screened at all. This app has no persistent background process — it just needs " +
                        "the system to be allowed to wake it briefly per call.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onRequestIgnoreBatteryOptimizations,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Exempt from battery optimization")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Allow calls from contacts")
            Switch(checked = allowContacts, onCheckedChange = {
                allowContacts = it
                settings.allowContacts = it
            })
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Allow recently accepted callers")
            Switch(checked = recentEnabled, onCheckedChange = {
                recentEnabled = it
                settings.recentAcceptedRuleEnabled = it
            })
        }
        if (recentEnabled) {
            Text("Allow again for ${recentDays.toInt()} days after you answer them")
            Slider(
                value = recentDays,
                onValueChange = { recentDays = it },
                onValueChangeFinished = { settings.allowRecentAcceptedDays = recentDays.toInt() },
                valueRange = 1f..90f
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Reject unknown numbers by default")
            Switch(checked = blockUnknown, onCheckedChange = {
                blockUnknown = it
                settings.blockUnknownNumbers = it
            })
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("When a call is blocked", style = MaterialTheme.typography.titleSmall)
        Text(
            "Applies to your block list and, if enabled above, unknown numbers.",
            style = MaterialTheme.typography.bodySmall
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            listOf(
                BlockedCallAction.SILENCE to "Show it silently — appears on screen/notifications, never rings",
                BlockedCallAction.REJECT to "Reject it outright — never surfaces as an incoming call"
            ).forEach { (action, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = blockedCallAction == action,
                            onClick = {
                                blockedCallAction = action
                                settings.blockedCallAction = action
                            }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = blockedCallAction == action,
                        onClick = {
                            blockedCallAction = action
                            settings.blockedCallAction = action
                        }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Debug-only: runs the same rule evaluation the real CallScreeningService uses, without a real call. */
@Composable
private fun DebugScreen(viewModel: MainViewModel) {
    var number by remember { mutableStateOf("") }
    val result by viewModel.simulationResult.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Simulate an incoming call to test your rules without dialing your device.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Phone number") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Button(
            onClick = { viewModel.simulateIncomingCall(number) },
            enabled = number.isNotBlank(),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Simulate call")
        }
        result?.let {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (it.startsWith("ALLOWED")) Icons.Filled.CheckCircle else Icons.Filled.Block,
                    contentDescription = null
                )
                Text(it, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
