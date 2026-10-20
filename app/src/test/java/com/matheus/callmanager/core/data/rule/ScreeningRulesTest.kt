package com.matheus.callmanager.core.data.rule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.entity.AcceptedCaller
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.testing.FakeContactsLookup
import com.matheus.callmanager.testing.FakeRecentOutgoingLookup
import com.matheus.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreeningRulesTest {
    private lateinit var db: CallManagerDatabase
    private val contacts = FakeContactsLookup()
    private val outgoing = FakeRecentOutgoingLookup()
    private val now = 1_000_000_000_000L
    private val number = "+15550001111"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun ctx(
        prefs: UserPreferences = UserPreferences(),
        callerName: String? = null,
    ) = ScreeningContext(number, callerName, prefs, now)

    private fun daysAgo(days: Int) = now - TimeUnit.DAYS.toMillis(days.toLong())

    @Test
    fun `BlockListRule rejects listed numbers and defers otherwise`() =
        runTest {
            val rule = BlockListRule(db.blockListDao())
            assertNull(rule.evaluate(ctx()))

            db.blockListDao().insertAll(listOf(BlockListEntry(number, null, now)))

            assertEquals(ScreeningDecision.Reject("On your block list"), rule.evaluate(ctx()))
        }

    @Test
    fun `AllowListRule allows listed numbers and defers otherwise`() =
        runTest {
            val rule = AllowListRule(db.allowListDao())
            assertNull(rule.evaluate(ctx()))

            db.allowListDao().insertAll(listOf(AllowListEntry(number, null, now)))

            assertEquals(ScreeningDecision.Allow("On your allow list"), rule.evaluate(ctx()))
        }

    @Test
    fun `NamePatternRule allows a matching caller name and defers otherwise`() =
        runTest {
            val rule = NamePatternRule(db.allowedNamePatternDao())
            db.allowedNamePatternDao().insert(AllowedNamePattern("Dr.", now))

            assertEquals(
                ScreeningDecision.Allow("Caller name matches an allow pattern"),
                rule.evaluate(ctx(callerName = "Dr. Smith")),
            )
            assertNull(rule.evaluate(ctx(callerName = "Telemarketer")))
            assertNull(rule.evaluate(ctx(callerName = null)))
        }

    @Test
    fun `ContactRule respects the allowContacts toggle`() =
        runTest {
            val rule = ContactRule(contacts)
            contacts.contacts[number] = "Friend"

            assertEquals(ScreeningDecision.Allow("Known contact"), rule.evaluate(ctx()))
            assertNull(rule.evaluate(ctx(UserPreferences(allowContacts = false))))
            assertNull(ContactRule(FakeContactsLookup()).evaluate(ctx()))
        }

    @Test
    fun `RecentlyAcceptedRule honours the toggle and the day window`() =
        runTest {
            val rule = RecentlyAcceptedRule(db.acceptedCallerDao())
            assertNull(rule.evaluate(ctx()))

            db.acceptedCallerDao().upsert(AcceptedCaller(number, daysAgo(5)))
            assertEquals(ScreeningDecision.Allow("Accepted within the last 30 days"), rule.evaluate(ctx()))
            assertNull(rule.evaluate(ctx(UserPreferences(recentAcceptedRuleEnabled = false))))
            assertNull(rule.evaluate(ctx(UserPreferences(allowRecentAcceptedDays = 1))))
        }

    @Test
    fun `RecentlyCalledRule honours the toggle and the day window`() =
        runTest {
            val rule = RecentlyCalledRule(outgoing)
            assertNull(rule.evaluate(ctx()))

            outgoing.outgoingCalls[number] = daysAgo(5)
            assertEquals(ScreeningDecision.Allow("You called them within the last 30 days"), rule.evaluate(ctx()))
            assertNull(rule.evaluate(ctx(UserPreferences(recentOutgoingRuleEnabled = false))))
            assertNull(rule.evaluate(ctx(UserPreferences(allowRecentOutgoingDays = 1))))
        }

    @Test
    fun `UnknownNumberFallbackRule always decides based on blockUnknownNumbers`() =
        runTest {
            val rule = UnknownNumberFallbackRule()

            assertEquals(ScreeningDecision.Allow("No matching block rule"), rule.evaluate(ctx()))
            assertEquals(
                ScreeningDecision.Reject("Unknown number"),
                rule.evaluate(ctx(UserPreferences(blockUnknownNumbers = true))),
            )
        }
}
