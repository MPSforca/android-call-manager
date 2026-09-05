package com.matheus.callmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import java.text.DateFormat
import java.util.Date
import androidx.compose.material.icons.filled.Delete

private enum class Tab(val label: String) { RECENT("Recent"), BLOCKED("Blocked"), RULES("Rules") }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CallManagerApp(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(Tab.RECENT) }

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
                    selected = tab == Tab.BLOCKED,
                    onClick = { tab = Tab.BLOCKED },
                    icon = { Icon(Icons.Filled.Block, contentDescription = null) },
                    label = { Text(Tab.BLOCKED.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.RULES,
                    onClick = { tab = Tab.RULES },
                    icon = { Icon(Icons.Filled.Rule, contentDescription = null) },
                    label = { Text(Tab.RULES.label) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.RECENT -> RecentCallsScreen(viewModel)
                Tab.BLOCKED -> BlockedScreen(viewModel)
                Tab.RULES -> RulesScreen(viewModel)
            }
        }
    }
}

@Composable
private fun RecentCallsScreen(viewModel: MainViewModel) {
    val calls by viewModel.recentCalls.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Select calls to block in bulk")
            if (selected.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    viewModel.blockNumbers(selected)
                    selected = emptySet()
                }) {
                    Icon(Icons.Filled.Block, contentDescription = "Block selected")
                }
            }
        }
        LazyColumn {
            items(calls, key = { it.timestamp }) { call ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected.contains(call.number),
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + call.number else selected - call.number
                        }
                    )
                    Column {
                        Text(call.name ?: call.number, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${call.number} · ${DateFormat.getDateTimeInstance().format(Date(call.timestamp))}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BlockedScreen(viewModel: MainViewModel) {
    val blockList by viewModel.blockList.collectAsState()
    val log by viewModel.blockedCallLog.collectAsState()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { Text("Block list", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
        items(blockList, key = { it.number }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(entry.number)
                IconButton(onClick = { viewModel.unblock(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item { Text("Rejected calls", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
        items(log, key = { it.id }) { entry ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(entry.callerName ?: entry.number)
                Text(
                    "${entry.reason} · ${DateFormat.getDateTimeInstance().format(Date(entry.timestamp))}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RulesScreen(viewModel: MainViewModel) {
    val settings = viewModel.settings
    var allowContacts by remember { mutableStateOf(settings.allowContacts) }
    var recentEnabled by remember { mutableStateOf(settings.recentAcceptedRuleEnabled) }
    var recentDays by remember { mutableStateOf(settings.allowRecentAcceptedDays.toFloat()) }
    var blockUnknown by remember { mutableStateOf(settings.blockUnknownNumbers) }

    Column(modifier = Modifier.padding(16.dp)) {
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
    }
}
