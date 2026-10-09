package com.matheus.callmanager.feature.rules

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.datastore.SettingsStore
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RulesViewModelTest {
    private lateinit var db: CallManagerDatabase
    private lateinit var repository: CallRepository
    private lateinit var viewModel: RulesViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java).allowMainThreadQueries().build()
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), SettingsStore(context))
        viewModel = RulesViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `exposes the repository's settings store`() {
        assertSame(repository.settings, viewModel.settings)
    }

    @Test
    fun `settings changes made through the ViewModel are visible on the repository`() {
        viewModel.settings.blockUnknownNumbers = true

        assertTrue(repository.settings.blockUnknownNumbers)
    }
}
