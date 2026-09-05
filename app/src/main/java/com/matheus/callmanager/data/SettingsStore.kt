package com.matheus.callmanager.data

import android.content.Context

/** Simple SharedPreferences-backed store for the user's screening rule preferences. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("call_manager_settings", Context.MODE_PRIVATE)

    var allowContacts: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_CONTACTS, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_CONTACTS, value).apply()

    /** 0 disables the "recently accepted" rule. */
    var allowRecentAcceptedDays: Int
        get() = prefs.getInt(KEY_RECENT_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_RECENT_DAYS, value).apply()

    var recentAcceptedRuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_RECENT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_RECENT_ENABLED, value).apply()

    /** When true, any number not covered by an allow rule and not on the block list is rejected too. */
    var blockUnknownNumbers: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_UNKNOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_UNKNOWN, value).apply()

    private companion object {
        const val KEY_ALLOW_CONTACTS = "allow_contacts"
        const val KEY_RECENT_DAYS = "recent_accepted_days"
        const val KEY_RECENT_ENABLED = "recent_accepted_enabled"
        const val KEY_BLOCK_UNKNOWN = "block_unknown_numbers"
    }
}
