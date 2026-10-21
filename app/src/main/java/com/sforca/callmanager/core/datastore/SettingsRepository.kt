package com.sforca.callmanager.core.datastore

import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.ThemeMode
import com.sforca.callmanager.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Reactive, suspend-based access to the user's screening settings. */
interface SettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setAllowContacts(value: Boolean)

    suspend fun setRecentAcceptedRuleEnabled(value: Boolean)

    suspend fun setAllowRecentAcceptedDays(value: Int)

    suspend fun setRecentOutgoingRuleEnabled(value: Boolean)

    suspend fun setAllowRecentOutgoingDays(value: Int)

    suspend fun setBlockUnknownNumbers(value: Boolean)

    suspend fun setBlockedCallAction(value: BlockedCallAction)

    suspend fun setShowRejectedCallNotification(value: Boolean)

    suspend fun setDynamicColor(value: Boolean)

    suspend fun setThemeMode(value: ThemeMode)

    suspend fun setRulesSetupCompleted(value: Boolean)
}
