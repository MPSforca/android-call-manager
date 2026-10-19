package com.matheus.callmanager.feature.lists

import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import com.matheus.callmanager.core.database.entity.BlockListEntry

enum class ListTab { Blocked, Allowed, NamePatterns }

/**
 * Everything [ListsScreen] renders. The lists are already filtered by [query]; [totalInTab] is the
 * unfiltered size of the selected tab, so the screen can tell "empty" apart from "no matches".
 */
data class ListsUiState(
    val tab: ListTab = ListTab.Blocked,
    val query: String = "",
    val blocked: List<BlockListEntry> = emptyList(),
    val allowed: List<AllowListEntry> = emptyList(),
    val namePatterns: List<AllowedNamePattern> = emptyList(),
    val totalInTab: Int = 0,
)
