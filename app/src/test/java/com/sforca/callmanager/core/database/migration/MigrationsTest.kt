package com.sforca.callmanager.core.database.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.core.database.CallManagerDatabase
import com.sforca.callmanager.core.database.entity.AllowedNamePattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real v2 database file by hand (the v2 schema was never exported) and opens it with the
 * production v3 [CallManagerDatabase] and ONLY [MIGRATION_2_3] registered — no destructive
 * fallback — so a broken migration fails the test instead of silently wiping user data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationsTest {
    private lateinit var context: Context
    private var db: CallManagerDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        createV2Database()
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun createV2Database() {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null).use { sqlite ->
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS `block_list` " +
                    "(`number` TEXT NOT NULL, `displayName` TEXT, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`number`))",
            )
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS `allow_list` " +
                    "(`number` TEXT NOT NULL, `displayName` TEXT, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`number`))",
            )
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS `accepted_callers` " +
                    "(`number` TEXT NOT NULL, `lastAcceptedAt` INTEGER NOT NULL, PRIMARY KEY(`number`))",
            )
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS `blocked_call_log` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                    "`callerName` TEXT, `timestamp` INTEGER NOT NULL, `reason` TEXT NOT NULL)",
            )
            sqlite.execSQL("INSERT INTO block_list VALUES ('+15551110000', 'Spammer', 100)")
            sqlite.execSQL("INSERT INTO allow_list VALUES ('+15552220000', NULL, 200)")
            sqlite.execSQL("INSERT INTO accepted_callers VALUES ('+15553330000', 300)")
            sqlite.execSQL("INSERT INTO blocked_call_log VALUES (1, '+15551110000', NULL, 400, 'On your block list')")
            sqlite.version = 2
        }
    }

    private fun openMigrated(): CallManagerDatabase =
        Room
            .databaseBuilder(context, CallManagerDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

    @Test
    fun `migrating 2 to 3 preserves every pre-existing row`() =
        runTest {
            val migrated = openMigrated()

            assertEquals(
                "+15551110000",
                migrated
                    .blockListDao()
                    .observeAll()
                    .first()
                    .single()
                    .number,
            )
            assertEquals(
                "+15552220000",
                migrated
                    .allowListDao()
                    .observeAll()
                    .first()
                    .single()
                    .number,
            )
            assertEquals(300L, migrated.acceptedCallerDao().get("+15553330000")?.lastAcceptedAt)
            assertEquals(
                "On your block list",
                migrated
                    .blockedCallLogDao()
                    .observeAll()
                    .first()
                    .single()
                    .reason,
            )
        }

    @Test
    fun `migrating 2 to 3 creates a usable allowed_name_patterns table`() =
        runTest {
            val migrated = openMigrated()

            assertEquals(emptyList<String>(), migrated.allowedNamePatternDao().getAllPatterns())
            migrated.allowedNamePatternDao().insert(AllowedNamePattern("PagerDuty", 500))

            assertEquals(listOf("PagerDuty"), migrated.allowedNamePatternDao().getAllPatterns())
        }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
