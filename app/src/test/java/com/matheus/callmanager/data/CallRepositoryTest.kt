package com.matheus.callmanager.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeContactsLookup : ContactsLookup {
    val contacts = mutableMapOf<String, String>()
    override fun isContact(number: String): Boolean = contacts.containsKey(number)
    override fun displayName(number: String): String? = contacts[number]
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallRepositoryTest {
    private lateinit var db: CallManagerDatabase
    private lateinit var contactsLookup: FakeContactsLookup
    private lateinit var repository: CallRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contactsLookup = FakeContactsLookup()
        repository = CallRepository(context, database = db, contactsLookup = contactsLookup)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `unknown number is allowed by default`() = runTest {
        val decision = repository.evaluate("+15550000000")
        assertTrue(decision is ScreeningDecision.Allow)
    }

    @Test
    fun `number on block list is rejected even if it is also a contact`() = runTest {
        contactsLookup.contacts["+15551111111"] = "Some Contact"
        repository.blockNumbers(listOf("+15551111111" to "Some Contact"))

        val decision = repository.evaluate("+15551111111")

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `number on allow list is allowed even when block-unknown is on`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.allowNumbers(listOf("+15552222222" to "Allowed Caller"))

        val decision = repository.evaluate("+15552222222")

        assertTrue(decision is ScreeningDecision.Allow)
    }

    @Test
    fun `blocking a number removes any conflicting allow-list entry`() = runTest {
        repository.allowNumbers(listOf("+15553333333" to "Was Allowed"))
        repository.blockNumbers(listOf("+15553333333" to "Now Blocked"))

        val decision = repository.evaluate("+15553333333")

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `known contact is allowed when the contacts rule is enabled`() = runTest {
        repository.settings.allowContacts = true
        contactsLookup.contacts["+15554444444"] = "Mom"

        val decision = repository.evaluate("+15554444444")

        assertTrue(decision is ScreeningDecision.Allow)
    }

    @Test
    fun `contacts rule is ignored when disabled`() = runTest {
        repository.settings.allowContacts = false
        repository.settings.blockUnknownNumbers = true
        contactsLookup.contacts["+15555555555"] = "Mom"

        val decision = repository.evaluate("+15555555555")

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `caller accepted within the configured window is allowed again`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.settings.recentAcceptedRuleEnabled = true
        repository.settings.allowRecentAcceptedDays = 30
        repository.recordAccepted("+15556666666")

        val decision = repository.evaluate("+15556666666")

        assertTrue(decision is ScreeningDecision.Allow)
    }

    @Test
    fun `caller accepted outside the configured window is not allowed`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.settings.recentAcceptedRuleEnabled = true
        repository.settings.allowRecentAcceptedDays = 1

        // Simulate an acceptance from 5 days ago by writing directly through the DAO.
        val fiveDaysAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(5)
        db.acceptedCallerDao().upsert(AcceptedCaller("+15557777777", fiveDaysAgo))

        val decision = repository.evaluate("+15557777777")

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `blocking a number with no name falls back to the contact's name`() = runTest {
        contactsLookup.contacts["+15559999999"] = "Known Caller"

        repository.blockNumbers(listOf("+15559999999" to null))

        val entry = repository.observeBlockList().first().first()
        assertEquals("Known Caller", entry.displayName)
    }

    @Test
    fun `allowing a number with no name falls back to the contact's name`() = runTest {
        contactsLookup.contacts["+15550101010"] = "Known Caller"

        repository.allowNumbers(listOf("+15550101010" to null))

        val entry = repository.observeAllowList().first().first()
        assertEquals("Known Caller", entry.displayName)
    }

    @Test
    fun `caller whose display name matches an allow-name pattern is allowed even when block-unknown is on`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.addAllowedNamePattern("PagerDuty")

        val decision = repository.evaluate("+15550101011", "PagerDuty Outgoing Numbers")

        assertTrue(decision is ScreeningDecision.Allow)
    }

    @Test
    fun `caller display name that does not match any allow-name pattern falls through to block-unknown`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.addAllowedNamePattern("PagerDuty")

        val decision = repository.evaluate("+15550101012", "Some Spammer")

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `missing caller display name falls through to block-unknown even with allow-name patterns configured`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.addAllowedNamePattern("PagerDuty")

        val decision = repository.evaluate("+15550101013", null)

        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `removing an allow-name pattern stops it from matching`() = runTest {
        repository.settings.blockUnknownNumbers = true
        repository.addAllowedNamePattern("PagerDuty")
        val entry = repository.observeAllowedNamePatterns().first().first()

        repository.removeAllowedNamePattern(entry)

        val decision = repository.evaluate("+15550101014", "PagerDuty Outgoing Numbers")
        assertTrue(decision is ScreeningDecision.Reject)
    }

    @Test
    fun `rejecting a call logs it so it stays visible in the app`() = runTest {
        repository.blockNumbers(listOf("+15558888888" to "Spammer"))
        val decision = repository.evaluate("+15558888888") as ScreeningDecision.Reject
        repository.logBlockedCall("+15558888888", decision.reason)

        val log = repository.observeBlockedCallLog().first()

        assertEquals(1, log.size)
        assertEquals("+15558888888", log.first().number)
        assertEquals("On your block list", log.first().reason)
    }
}
