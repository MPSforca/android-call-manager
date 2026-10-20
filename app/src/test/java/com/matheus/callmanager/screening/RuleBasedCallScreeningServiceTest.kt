package com.matheus.callmanager.screening

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService.CallResponse
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.datastore.SettingsRepository
import com.matheus.callmanager.core.di.DatabaseModule
import com.matheus.callmanager.core.di.SettingsBindingModule
import com.matheus.callmanager.core.di.TelephonyModule
import com.matheus.callmanager.core.model.BlockedCallAction
import com.matheus.callmanager.core.telephony.CallLogDataSource
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import com.matheus.callmanager.testing.FakeCallLogDataSource
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.FakeSettingsRepository
import com.matheus.callmanager.testing.inMemoryTestDatabase
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Drives the real service through Hilt (cold-start injection) with the real [com.matheus.callmanager.core.data.CallRepository]
 * on an in-memory database and fake system seams. Telecom's reply goes through `respondToCall`, which needs a
 * bound Telecom adapter, so it is stubbed on a spy and captured.
 */
@HiltAndroidTest
@UninstallModules(DatabaseModule::class, SettingsBindingModule::class, TelephonyModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34])
class RuleBasedCallScreeningServiceTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @BindValue
    lateinit var db: CallManagerDatabase

    private val settings = FakeSettingsRepository()

    @BindValue
    @JvmField
    val settingsBinding: SettingsRepository = settings

    @BindValue
    @JvmField
    val contacts: ContactsLookup = FakeContactsLookup()

    @BindValue
    @JvmField
    val outgoing: RecentOutgoingLookup = FakeRecentOutgoingLookup()

    @BindValue
    @JvmField
    val callLog: CallLogDataSource = FakeCallLogDataSource()

    private val responses = CopyOnWriteArrayList<CallResponse>()
    private lateinit var service: RuleBasedCallScreeningService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        hilt.inject()
        service = spyk(Robolectric.buildService(RuleBasedCallScreeningService::class.java).create().get())
        val captured = slot<CallResponse>()
        every { service.respondToCall(any(), capture(captured)) } answers { responses += captured.captured }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun details(
        number: String?,
        selfManaged: Boolean = false,
        callerName: String? = null,
    ): Call.Details =
        mockk {
            every { handle } returns number?.let { Uri.fromParts("tel", it, null) }
            every { callProperties } returns if (selfManaged) Call.Details.PROPERTY_SELF_MANAGED else 0
            every { callerDisplayName } returns callerName
        }

    // Plain SQL on the test thread: no coroutines or Room executors, so a stuck service can only fail a bounded wait.
    private fun block(number: String) =
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO block_list (number, displayName, addedAt) VALUES (?, NULL, 1)",
            arrayOf(number),
        )

    private fun loggedNumbers(): List<String> =
        db.openHelper.readableDatabase.query("SELECT number FROM blocked_call_log").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
        }

    private fun awaitResponse(): CallResponse {
        awaitUntil { responses.isNotEmpty() }
        return responses.single()
    }

    private fun awaitLogCount(count: Int) =
        awaitUntil {
            loggedNumbers().size == count
        }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertTrue("condition not met within ${TIMEOUT_MS}ms", condition())
    }

    private fun notifications() =
        shadowOf(
            ApplicationProvider.getApplicationContext<Context>().getSystemService(NotificationManager::class.java),
        ).allNotifications

    @Test
    fun `cold start injects the repository`() {
        assertNotNull(service.repository)
    }

    @Test
    fun `unblocked number with blocking of unknowns off is allowed and not logged`() {
        service.onScreenCall(details("+15551230000"))

        val response = awaitResponse()

        assertFalse(response.disallowCall)
        assertFalse(response.silenceCall)
        assertEquals(
            0,
            loggedNumbers().size,
        )
    }

    @Test
    fun `blocked number is silenced by default and logged`() {
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        val response = awaitResponse()
        assertTrue(response.silenceCall)
        assertFalse(response.disallowCall)
        awaitLogCount(1)
        assertEquals(listOf("+15551230000"), loggedNumbers())
    }

    @Test
    fun `silenced call posts no notification on modern Android`() {
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        awaitResponse()
        awaitLogCount(1)
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `blocked number is rejected when action is reject`() {
        settings.update { copy(blockedCallAction = BlockedCallAction.REJECT) }
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        val response = awaitResponse()
        assertTrue(response.disallowCall)
        assertTrue(response.rejectCall)
        assertTrue(response.skipNotification)
        assertFalse(response.silenceCall)
        awaitLogCount(1)
        awaitUntil { notifications().isNotEmpty() }
    }

    @Test
    fun `rejected call posts a notification naming the contact`() {
        settings.update { copy(blockedCallAction = BlockedCallAction.REJECT) }
        (contacts as FakeContactsLookup).contacts["+15551230000"] = "Alice"
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        awaitUntil { notifications().isNotEmpty() }
        val notification = notifications().single()
        assertEquals("Blocked call from Alice", notification.extras.getString("android.title"))
    }

    @Test
    fun `rejected call posts no notification when the preference is off`() {
        settings.update { copy(blockedCallAction = BlockedCallAction.REJECT, showRejectedCallNotification = false) }
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        awaitResponse()
        awaitLogCount(1)
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `unknown number is rejected when blocking unknowns`() {
        settings.update { copy(blockUnknownNumbers = true, blockedCallAction = BlockedCallAction.REJECT) }

        service.onScreenCall(details("+15559990000"))

        assertTrue(awaitResponse().disallowCall)
        awaitLogCount(1)
        awaitUntil { notifications().isNotEmpty() }
    }

    @Test
    fun `self-managed call is allowed without evaluation`() {
        block("+15551230000")

        service.onScreenCall(details("+15551230000", selfManaged = true))

        val response = awaitResponse()
        assertFalse(response.disallowCall)
        assertFalse(response.silenceCall)
        assertEquals(
            0,
            loggedNumbers().size,
        )
    }

    @Test
    fun `call without a handle is allowed`() {
        service.onScreenCall(details(null))

        assertFalse(awaitResponse().disallowCall)
    }

    @Test
    @Config(application = HiltTestApplication::class, sdk = [28])
    fun `silence falls back to a full reject with a notification below API 29`() {
        block("+15551230000")

        service.onScreenCall(details("+15551230000"))

        val response = awaitResponse()
        assertTrue(response.disallowCall)
        assertTrue(response.rejectCall)
        awaitUntil { notifications().isNotEmpty() }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
    }
}
