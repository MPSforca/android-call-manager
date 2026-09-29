package com.matheus.callmanager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.matheus.callmanager.feature.debug.DebugScreen
import com.matheus.callmanager.feature.lists.ListsScreen
import com.matheus.callmanager.feature.recent.RecentScreen
import com.matheus.callmanager.feature.rules.RulesScreen

private enum class Tab(
    val label: String,
) {
    RECENT("Recent"),
    LISTS("Lists"),
    RULES("Rules"),
    DEBUG("Debug"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallManagerApp(
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
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
                    label = { Text(Tab.RECENT.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.LISTS,
                    onClick = { tab = Tab.LISTS },
                    icon = { Icon(Icons.Filled.Block, contentDescription = null) },
                    label = { Text(Tab.LISTS.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.RULES,
                    onClick = { tab = Tab.RULES },
                    icon = { Icon(Icons.Filled.Rule, contentDescription = null) },
                    label = { Text(Tab.RULES.label) },
                )
                if (showDebugTab) {
                    NavigationBarItem(
                        selected = tab == Tab.DEBUG,
                        onClick = { tab = Tab.DEBUG },
                        icon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                        label = { Text(Tab.DEBUG.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.RECENT -> RecentScreen()
                Tab.LISTS -> ListsScreen(onPickContactToBlock, onPickContactToAllow)
                Tab.RULES -> RulesScreen(isIgnoringBatteryOptimizations, onRequestIgnoreBatteryOptimizations)
                Tab.DEBUG -> DebugScreen()
            }
        }
    }
}
