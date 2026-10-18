package com.matheus.callmanager.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun dataStore(
        file: File = tmp.newFile("settings.preferences_pb").also { it.delete() },
        withMigration: Boolean = false,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            migrations =
                if (withMigration) {
                    listOf(SharedPreferencesMigration(context, DataStoreSettingsRepository.LEGACY_PREFS_NAME))
                } else {
                    emptyList()
                },
            produceFile = { file },
        )

    @Test
    fun `fresh install exposes the default preferences`() =
        runTest {
            val repo = DataStoreSettingsRepository(dataStore())

            assertEquals(UserPreferences(), repo.preferences.first())
        }

    @Test
    fun `every setter is reflected in the preferences flow`() =
        runTest {
            val repo = DataStoreSettingsRepository(dataStore())

            repo.setAllowContacts(false)
            repo.setRecentAcceptedRuleEnabled(false)
            repo.setAllowRecentAcceptedDays(7)
            repo.setRecentOutgoingRuleEnabled(false)
            repo.setAllowRecentOutgoingDays(9)
            repo.setBlockUnknownNumbers(true)
            repo.setBlockedCallAction(BlockedCallAction.REJECT)
            repo.setShowRejectedCallNotification(false)
            repo.setDynamicColor(true)

            assertEquals(
                UserPreferences(
                    allowContacts = false,
                    recentAcceptedRuleEnabled = false,
                    allowRecentAcceptedDays = 7,
                    recentOutgoingRuleEnabled = false,
                    allowRecentOutgoingDays = 9,
                    blockUnknownNumbers = true,
                    blockedCallAction = BlockedCallAction.REJECT,
                    showRejectedCallNotification = false,
                    dynamicColor = true,
                ),
                repo.preferences.first(),
            )
        }

    @Test
    fun `preferences flow emits again when a setting changes`() =
        runTest {
            val repo = DataStoreSettingsRepository(dataStore())

            repo.preferences.test {
                assertEquals(UserPreferences(), awaitItem())
                repo.setBlockUnknownNumbers(true)
                assertEquals(UserPreferences(blockUnknownNumbers = true), awaitItem())
            }
        }

    @Test
    fun `an unrecognised stored blocked-call action falls back to the default`() =
        runTest {
            val store = dataStore()
            store.edit { it[stringPreferencesKey("blocked_call_action")] = "NOT_AN_ACTION" }

            assertEquals(
                BlockedCallAction.SILENCE,
                DataStoreSettingsRepository(store).preferences.first().blockedCallAction,
            )
        }

    @Test
    fun `legacy SharedPreferences values are migrated into DataStore`() =
        runTest {
            context
                .getSharedPreferences(DataStoreSettingsRepository.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("allow_contacts", false)
                .putInt("recent_accepted_days", 12)
                .putBoolean("recent_accepted_enabled", false)
                .putInt("recent_outgoing_days", 3)
                .putBoolean("recent_outgoing_enabled", false)
                .putBoolean("block_unknown_numbers", true)
                .putString("blocked_call_action", "REJECT")
                .putBoolean("show_rejected_call_notification", false)
                .commit()

            val repo = DataStoreSettingsRepository(dataStore(withMigration = true))

            assertEquals(
                UserPreferences(
                    allowContacts = false,
                    recentAcceptedRuleEnabled = false,
                    allowRecentAcceptedDays = 12,
                    recentOutgoingRuleEnabled = false,
                    allowRecentOutgoingDays = 3,
                    blockUnknownNumbers = true,
                    blockedCallAction = BlockedCallAction.REJECT,
                    showRejectedCallNotification = false,
                ),
                repo.preferences.first(),
            )
        }
}
