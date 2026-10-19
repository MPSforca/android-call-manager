package com.matheus.callmanager.feature.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmPhoneTextField
import com.matheus.callmanager.core.designsystem.component.CmSegmentedButton
import com.matheus.callmanager.core.designsystem.component.CmTextButton
import com.matheus.callmanager.core.designsystem.component.EmptyState
import com.matheus.callmanager.core.designsystem.component.SwipeToDismissRow
import com.matheus.callmanager.core.designsystem.component.showUndo
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ListsContent(
        state = state,
        onPickContactToBlock = onPickContactToBlock,
        onPickContactToAllow = onPickContactToAllow,
        viewModel = viewModel,
    )
}

/** One removable row, whatever list it comes from. */
private class ListRow(
    val key: String,
    val title: String,
    val subtitle: String?,
    val remove: () -> Unit,
    val restore: () -> Unit,
)

/** Stateless body of [ListsScreen]; [viewModel] only receives the user's edits. */
@Composable
internal fun ListsContent(
    state: ListsUiState,
    onPickContactToBlock: () -> Unit,
    onPickContactToAllow: () -> Unit,
    viewModel: ListsViewModel,
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val region = remember(context) { PhoneNumberFormatter.defaultRegion(context) }
    val snackbarHost = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    val rows = remember(state, region) { buildRows(state, context, region, viewModel) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CmSegmentedButton(
                options = ListTab.entries,
                selected = state.tab,
                onSelect = viewModel::selectTab,
                label = { context.getString(it.labelRes()) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
            )
            SearchField(state.query, viewModel::setQuery)
            Text(
                text = stringResource(state.tab.subtitleRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
            )
            if (rows.isEmpty()) {
                ListEmptyState(state)
            } else {
                RowList(rows, state.tab, snackbarHost)
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.large),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.lists_add))
        }
        SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddDialog) {
        AddEntryDialog(
            tab = state.tab,
            onDismiss = { showAddDialog = false },
            onSubmit = { text ->
                submitEntry(state.tab, text, context, region, viewModel)
                showAddDialog = false
            },
            onPickContact =
                when (state.tab) {
                    ListTab.Blocked -> onPickContactToBlock
                    ListTab.Allowed -> onPickContactToAllow
                    ListTab.NamePatterns -> null
                },
        )
    }
}

private fun buildRows(
    state: ListsUiState,
    context: android.content.Context,
    region: String,
    viewModel: ListsViewModel,
): List<ListRow> =
    when (state.tab) {
        ListTab.Blocked ->
            state.blocked.map { entry ->
                numberRow(entry.number, entry.displayName, context, region, { viewModel.unblock(entry) }) {
                    viewModel.blockContact(entry.number, entry.displayName)
                }
            }
        ListTab.Allowed ->
            state.allowed.map { entry ->
                numberRow(entry.number, entry.displayName, context, region, { viewModel.unallow(entry) }) {
                    viewModel.allowContact(entry.number, entry.displayName)
                }
            }
        ListTab.NamePatterns ->
            state.namePatterns.map { entry ->
                ListRow(
                    key = entry.pattern,
                    title = entry.pattern,
                    subtitle = null,
                    remove = { viewModel.removeAllowedNamePattern(entry) },
                    restore = { viewModel.addAllowedNamePattern(entry.pattern) },
                )
            }
    }

private fun submitEntry(
    tab: ListTab,
    text: String,
    context: android.content.Context,
    region: String,
    viewModel: ListsViewModel,
) {
    when (tab) {
        ListTab.NamePatterns -> viewModel.addAllowedNamePattern(text)
        ListTab.Blocked -> viewModel.blockContact(PhoneNumberFormatter.normalizeForStorage(context, text, region), null)
        ListTab.Allowed -> viewModel.allowContact(PhoneNumberFormatter.normalizeForStorage(context, text, region), null)
    }
}

private fun numberRow(
    number: String,
    name: String?,
    context: android.content.Context,
    region: String,
    remove: () -> Unit,
    restore: () -> Unit,
): ListRow {
    val formatted = PhoneNumberFormatter.format(context, number, region)
    return ListRow(
        key = number,
        title = name ?: formatted,
        subtitle = if (name != null) formatted else null,
        remove = remove,
        restore = restore,
    )
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
        label = { Text(stringResource(R.string.lists_search_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.lists_search_clear))
                    }
                }
            } else {
                null
            },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
    )
}

@Composable
private fun ListEmptyState(state: ListsUiState) {
    if (state.totalInTab > 0) {
        EmptyState(icon = Icons.Filled.Search, title = stringResource(R.string.lists_no_matches))
        return
    }
    val (title, message) =
        when (state.tab) {
            ListTab.Blocked -> R.string.lists_empty_blocked_title to R.string.lists_empty_blocked_message
            ListTab.Allowed -> R.string.lists_empty_allowed_title to R.string.lists_empty_allowed_message
            ListTab.NamePatterns -> R.string.lists_empty_names_title to R.string.lists_empty_names_message
        }
    EmptyState(
        icon = if (state.tab == ListTab.Blocked) Icons.Filled.Block else Icons.Filled.Person,
        title = stringResource(title),
        message = stringResource(message),
    )
}

@Composable
private fun AddEntryDialog(
    tab: ListTab,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onPickContact: (() -> Unit)?,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (tab) {
                        ListTab.Blocked -> R.string.lists_add_blocked
                        ListTab.Allowed -> R.string.lists_add_allowed
                        ListTab.NamePatterns -> R.string.lists_add_name
                    },
                ),
            )
        },
        text = {
            Column {
                if (tab == ListTab.NamePatterns) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.lists_name_label)) },
                        singleLine = true,
                    )
                } else {
                    CmPhoneTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = stringResource(R.string.lists_number_label),
                    )
                }
                if (onPickContact != null) {
                    CmTextButton(
                        text = stringResource(R.string.lists_pick_contact),
                        onClick = {
                            onDismiss()
                            onPickContact()
                        },
                    )
                }
            }
        },
        confirmButton = {
            CmTextButton(
                text = stringResource(R.string.lists_add),
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank(),
            )
        },
        dismissButton = { CmTextButton(text = stringResource(R.string.lists_cancel), onClick = onDismiss) },
    )
}

private fun ListTab.labelRes() =
    when (this) {
        ListTab.Blocked -> R.string.lists_tab_blocked
        ListTab.Allowed -> R.string.lists_tab_allowed
        ListTab.NamePatterns -> R.string.lists_tab_names
    }

private fun ListTab.subtitleRes() =
    when (this) {
        ListTab.Blocked -> R.string.lists_blocked_subtitle
        ListTab.Allowed -> R.string.lists_allowed_subtitle
        ListTab.NamePatterns -> R.string.lists_names_subtitle
    }

@Composable
private fun RowList(
    rows: List<ListRow>,
    tab: ListTab,
    snackbarHost: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.lists_undo)
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.key }) { row ->
            SwipeToDismissRow(
                onDismiss = {
                    row.remove()
                    scope.launch {
                        snackbarHost.showUndo(
                            context.getString(R.string.lists_removed, row.title),
                            undoLabel,
                            row.restore,
                        )
                    }
                },
                deleteContentDescription = stringResource(R.string.lists_remove),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            if (tab ==
                                ListTab.Blocked
                            ) {
                                Icons.Filled.Block
                            } else {
                                Icons.Filled.Person
                            },
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(row.title) },
                    supportingContent = row.subtitle?.let { { Text(it) } },
                )
            }
        }
    }
}
