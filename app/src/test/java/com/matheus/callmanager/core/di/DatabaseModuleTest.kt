package com.matheus.callmanager.core.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.entity.BlockListEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseModuleTest {
    private lateinit var context: Context
    private lateinit var db: CallManagerDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = DatabaseModule.provideCallManagerDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("call-manager.db")
    }

    @Test
    fun `provided database is a working Room instance backed by the expected daos`() =
        runTest {
            db.blockListDao().insertAll(listOf(BlockListEntry("+15550000000", "Test", 0L)))

            val entries = db.blockListDao().observeAll().first()

            assertEquals(1, entries.size)
            assertEquals("+15550000000", entries.first().number)
        }
}
