package com.matheus.callmanager.screening

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.di.DatabaseModule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The RINGING -> OFFHOOK path that records "accepted" callers; invisible to the rest of the suite. */
@HiltAndroidTest
@UninstallModules(DatabaseModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34])
class PhoneStateReceiverTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @BindValue
    lateinit var db: CallManagerDatabase

    private lateinit var context: Context
    private val receiver = PhoneStateReceiver()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java).allowMainThreadQueries().build()
        hilt.inject()
        // The last-ringing number is process-wide state; start every test from IDLE.
        send(TelephonyManager.EXTRA_STATE_IDLE)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun send(
        state: String?,
        number: String? = null,
    ) {
        val intent = Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        state?.let { intent.putExtra(TelephonyManager.EXTRA_STATE, it) }
        number?.let { intent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, it) }
        receiver.onReceive(context, intent)
    }

    private fun accepted(number: String) = runBlocking { db.acceptedCallerDao().get(number) }

    private fun awaitAccepted(number: String) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (accepted(number) == null && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertNotNull("$number was not recorded as accepted", accepted(number))
    }

    /** Gives the receiver's fire-and-forget IO coroutine time to (not) write, then checks nothing landed. */
    private fun assertNeverAccepted(number: String) {
        Thread.sleep(SETTLE_MS)
        assertNull(accepted(number))
    }

    @Test
    fun `ringing then offhook records the caller as accepted`() {
        send(TelephonyManager.EXTRA_STATE_RINGING, "+15551230000")
        send(TelephonyManager.EXTRA_STATE_OFFHOOK)

        awaitAccepted("+15551230000")
    }

    @Test
    fun `ringing then idle does not record the caller`() {
        send(TelephonyManager.EXTRA_STATE_RINGING, "+15551230000")
        send(TelephonyManager.EXTRA_STATE_IDLE)
        send(TelephonyManager.EXTRA_STATE_OFFHOOK)

        assertNeverAccepted("+15551230000")
    }

    @Test
    fun `offhook without a ringing call records nothing`() {
        send(TelephonyManager.EXTRA_STATE_OFFHOOK)

        assertNeverAccepted("+15551230000")
    }

    @Test
    fun `private caller with no number records nothing`() {
        send(TelephonyManager.EXTRA_STATE_RINGING)
        send(TelephonyManager.EXTRA_STATE_OFFHOOK)

        assertNeverAccepted("+15551230000")
    }

    @Test
    fun `broadcast without a state is ignored`() {
        send(TelephonyManager.EXTRA_STATE_RINGING, "+15551230000")
        send(null)
        send(TelephonyManager.EXTRA_STATE_OFFHOOK)

        awaitAccepted("+15551230000")
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
        const val SETTLE_MS = 300L
    }
}
