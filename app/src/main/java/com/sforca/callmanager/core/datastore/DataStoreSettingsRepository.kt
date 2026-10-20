package com.sforca.callmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sforca.callmanager.core.model.BlockedCallAction
import com.sforca.callmanager.core.model.ThemeMode
import com.sforca.callmanager.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Key names intentionally match the legacy SharedPreferences file ([LEGACY_PREFS_NAME]) so
 * `SharedPreferencesMigration` carries existing installs' settings over without a mapping step.
 */
@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val preferences: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

        override suspend fun setAllowContacts(value: Boolean) = set(ALLOW_CONTACTS, value)

        override suspend fun setRecentAcceptedRuleEnabled(value: Boolean) = set(RECENT_ENABLED, value)

        override suspend fun setAllowRecentAcceptedDays(value: Int) = set(RECENT_DAYS, value)

        override suspend fun setRecentOutgoingRuleEnabled(value: Boolean) = set(OUTGOING_ENABLED, value)

        override suspend fun setAllowRecentOutgoingDays(value: Int) = set(OUTGOING_DAYS, value)

        override suspend fun setBlockUnknownNumbers(value: Boolean) = set(BLOCK_UNKNOWN, value)

        override suspend fun setBlockedCallAction(value: BlockedCallAction) = set(BLOCKED_ACTION, value.name)

        override suspend fun setShowRejectedCallNotification(value: Boolean) = set(SHOW_REJECTED_NOTIFICATION, value)

        override suspend fun setDynamicColor(value: Boolean) = set(DYNAMIC_COLOR, value)

        override suspend fun setThemeMode(value: ThemeMode) = set(THEME_MODE, value.name)

        private suspend fun <T> set(
            key: Preferences.Key<T>,
            value: T,
        ) {
            dataStore.edit { it[key] = value }
        }

        private fun Preferences.toUserPreferences(): UserPreferences {
            val defaults = UserPreferences()
            return UserPreferences(
                allowContacts = this[ALLOW_CONTACTS] ?: defaults.allowContacts,
                recentAcceptedRuleEnabled = this[RECENT_ENABLED] ?: defaults.recentAcceptedRuleEnabled,
                allowRecentAcceptedDays = this[RECENT_DAYS] ?: defaults.allowRecentAcceptedDays,
                recentOutgoingRuleEnabled = this[OUTGOING_ENABLED] ?: defaults.recentOutgoingRuleEnabled,
                allowRecentOutgoingDays = this[OUTGOING_DAYS] ?: defaults.allowRecentOutgoingDays,
                blockUnknownNumbers = this[BLOCK_UNKNOWN] ?: defaults.blockUnknownNumbers,
                blockedCallAction =
                    this[BLOCKED_ACTION]
                        ?.let { name -> BlockedCallAction.entries.firstOrNull { it.name == name } }
                        ?: defaults.blockedCallAction,
                showRejectedCallNotification =
                    this[SHOW_REJECTED_NOTIFICATION] ?: defaults.showRejectedCallNotification,
                dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
                themeMode =
                    this[THEME_MODE]
                        ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                        ?: defaults.themeMode,
            )
        }

        companion object {
            const val LEGACY_PREFS_NAME = "call_manager_settings"

            internal val ALLOW_CONTACTS = booleanPreferencesKey("allow_contacts")
            internal val RECENT_DAYS = intPreferencesKey("recent_accepted_days")
            internal val RECENT_ENABLED = booleanPreferencesKey("recent_accepted_enabled")
            internal val OUTGOING_DAYS = intPreferencesKey("recent_outgoing_days")
            internal val OUTGOING_ENABLED = booleanPreferencesKey("recent_outgoing_enabled")
            internal val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_numbers")
            internal val BLOCKED_ACTION = stringPreferencesKey("blocked_call_action")
            internal val SHOW_REJECTED_NOTIFICATION = booleanPreferencesKey("show_rejected_call_notification")
            internal val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
            internal val THEME_MODE = stringPreferencesKey("theme_mode")
        }
    }
