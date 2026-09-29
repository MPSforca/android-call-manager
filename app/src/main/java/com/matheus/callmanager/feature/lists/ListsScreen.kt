package com.matheus.callmanager.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun ListsScreen(
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val blockList by viewModel.blockList.collectAsState()
    val allowList by viewModel.allowList.collectAsState()
    val allowedNamePatterns by viewModel.allowedNamePatterns.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    val hasMoreBlockedCalls by viewModel.hasMoreBlockedCalls.collectAsState()
    var manualNumber by remember { mutableStateOf("") }
    var manualNamePattern by remember { mutableStateOf("") }
    val context = LocalContext.current
    val defaultRegion = remember(context) { PhoneNumberFormatter.defaultRegion(context) }
    val asYouTypeFormatter =
        remember(context, defaultRegion) {
            PhoneNumberFormatter.newAsYouTypeFormatter(context, defaultRegion)
        }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            OutlinedTextField(
                value = manualNumber,
                onValueChange = { manualNumber = PhoneNumberFormatter.formatAsYouType(asYouTypeFormatter, it) },
                label = { Text("Add a number manually") },
                placeholder = { Text("e.g. (19) 99999-9999 or +55 19 99999-9999") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val normalized = PhoneNumberFormatter.normalizeForStorage(context, manualNumber, defaultRegion)
                        viewModel.blockContact(normalized, null)
                        manualNumber = ""
                    },
                    enabled = manualNumber.isNotBlank(),
                ) { Text("Block it") }
                OutlinedButton(
                    onClick = {
                        val normalized = PhoneNumberFormatter.normalizeForStorage(context, manualNumber, defaultRegion)
                        viewModel.allowContact(normalized, null)
                        manualNumber = ""
                    },
                    enabled = manualNumber.isNotBlank(),
                ) { Text("Allow it") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Block list", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onPickContactToBlock) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact to block list")
                }
            }
        }
        items(blockList, key = { it.number }) { entry ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                headlineContent = {
                    Text(
                        entry.displayName ?: PhoneNumberFormatter.format(context, entry.number, defaultRegion),
                    )
                },
                trailingContent = {
                    IconButton(onClick = { viewModel.unblock(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Allow list", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onPickContactToAllow) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact to allow list")
                }
            }
        }
        items(allowList, key = { it.number }) { entry ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                headlineContent = {
                    Text(
                        entry.displayName ?: PhoneNumberFormatter.format(context, entry.number, defaultRegion),
                    )
                },
                trailingContent = {
                    IconButton(onClick = { viewModel.unallow(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Text("Allow calls by caller name", style = MaterialTheme.typography.titleMedium)
            Text(
                "Matches the caller ID name your carrier shows for the call (e.g. \"PagerDuty\") — " +
                    "useful for callers whose number rotates but whose name stays the same. Not " +
                    "every carrier or call provides a name.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = manualNamePattern,
                    onValueChange = { manualNamePattern = it },
                    label = { Text("Name contains") },
                    placeholder = { Text("e.g. PagerDuty") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = {
                        viewModel.addAllowedNamePattern(manualNamePattern)
                        manualNamePattern = ""
                    },
                    enabled = manualNamePattern.isNotBlank(),
                ) { Text("Add") }
            }
        }
        items(allowedNamePatterns, key = { it.pattern }) { entry ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                headlineContent = { Text(entry.pattern) },
                trailingContent = {
                    IconButton(onClick = { viewModel.removeAllowedNamePattern(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Text("Blocked calls", style = MaterialTheme.typography.titleMedium)
            Text(
                "Includes calls blocked by this app, Android's own block list, and your carrier.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(blockedCalls, key = { "${it.number}-${it.timestamp}" }) { entry ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Block, contentDescription = null) },
                headlineContent = {
                    Text(
                        entry.name ?: PhoneNumberFormatter.format(context, entry.number, defaultRegion),
                    )
                },
                supportingContent = {
                    Text(
                        "${entry.reason} · ${entry.source} · ${DateFormat.getDateTimeInstance().format(
                            Date(entry.timestamp),
                        )}",
                    )
                },
            )
        }
        if (hasMoreBlockedCalls) {
            item {
                OutlinedButton(
                    onClick = { viewModel.loadMoreBlockedCalls() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Load more") }
            }
        }
    }
}
