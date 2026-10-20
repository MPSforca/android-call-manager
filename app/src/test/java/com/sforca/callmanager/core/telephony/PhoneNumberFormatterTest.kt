package com.sforca.callmanager.core.telephony

import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneNumberFormatterTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `defaultRegion prefers the SIM country`() {
        val telephony = shadowOf(context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
        telephony.setSimCountryIso("br")
        telephony.setNetworkCountryIso("pt")

        assertEquals("BR", PhoneNumberFormatter.defaultRegion(context))
    }

    @Test
    fun `defaultRegion falls back to the network country`() {
        val telephony = shadowOf(context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
        telephony.setSimCountryIso("")
        telephony.setNetworkCountryIso("pt")

        assertEquals("PT", PhoneNumberFormatter.defaultRegion(context))
    }

    @Test
    fun `defaultRegion falls back to the locale, then US`() {
        val telephony = shadowOf(context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
        telephony.setSimCountryIso("")
        telephony.setNetworkCountryIso("")
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("es", "ES"))
            assertEquals("ES", PhoneNumberFormatter.defaultRegion(context))
            Locale.setDefault(Locale("en"))
            assertEquals("US", PhoneNumberFormatter.defaultRegion(context))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `format shows local numbers in national form`() {
        assertEquals("(650) 253-0000", PhoneNumberFormatter.format(context, "+16502530000", "US"))
        assertEquals("(650) 253-0000", PhoneNumberFormatter.format(context, "6502530000", "US"))
    }

    @Test
    fun `format shows foreign numbers in international form`() {
        assertEquals("+44 20 7946 0958", PhoneNumberFormatter.format(context, "+442079460958", "US"))
    }

    @Test
    fun `format returns input unchanged for short codes, garbage and empty input`() {
        assertEquals("911", PhoneNumberFormatter.format(context, "911", "US"))
        assertEquals("abc", PhoneNumberFormatter.format(context, "abc", "US"))
        assertEquals("", PhoneNumberFormatter.format(context, "", "US"))
        assertEquals("+1555", PhoneNumberFormatter.format(context, "+1555", "US"))
    }

    @Test
    fun `formatWithAreaCode splits the area code for geographic numbers`() {
        assertEquals("+1 (650)-2530000", PhoneNumberFormatter.formatWithAreaCode(context, "+16502530000", "US"))
    }

    @Test
    fun `formatWithAreaCode omits the area code where none applies`() {
        // UK mobile numbers have no geographic area code.
        assertEquals("+44 7912345678", PhoneNumberFormatter.formatWithAreaCode(context, "+447912345678", "GB"))
    }

    @Test
    fun `formatWithAreaCode returns input unchanged when invalid`() {
        assertEquals("911", PhoneNumberFormatter.formatWithAreaCode(context, "911", "US"))
        assertEquals("abc", PhoneNumberFormatter.formatWithAreaCode(context, "abc", "US"))
    }

    @Test
    fun `normalizeForStorage applies the default region`() {
        assertEquals("+5519999999999", PhoneNumberFormatter.normalizeForStorage(context, "19 99999-9999", "BR"))
        assertEquals("+16502530000", PhoneNumberFormatter.normalizeForStorage(context, "(650) 253-0000", "US"))
    }

    @Test
    fun `normalizeForStorage keeps an explicit country code`() {
        assertEquals("+442079460958", PhoneNumberFormatter.normalizeForStorage(context, "+44 20 7946 0958", "US"))
    }

    @Test
    fun `normalizeForStorage strips stray zeros after the plus`() {
        assertEquals("+5519999999999", PhoneNumberFormatter.normalizeForStorage(context, "+055 19 99999-9999", "BR"))
        assertEquals("+16502530000", PhoneNumberFormatter.normalizeForStorage(context, "+001 650 253 0000", "US"))
    }

    @Test
    fun `normalizeForStorage returns the trimmed input when it is not a valid number`() {
        assertEquals("911", PhoneNumberFormatter.normalizeForStorage(context, "  911 ", "US"))
        assertEquals("abc", PhoneNumberFormatter.normalizeForStorage(context, " abc", "US"))
        assertEquals("", PhoneNumberFormatter.normalizeForStorage(context, "   ", "US"))
    }

    @Test
    fun `formatAsYouType formats progressively and handles an empty input`() {
        val formatter = PhoneNumberFormatter.newAsYouTypeFormatter(context, "US")

        assertEquals("", PhoneNumberFormatter.formatAsYouType(formatter, ""))
        assertEquals("(650) 253-0000", PhoneNumberFormatter.formatAsYouType(formatter, "6502530000"))
        assertEquals("650-25", PhoneNumberFormatter.formatAsYouType(formatter, "65025"))
    }

    @Test
    fun `formatAsYouType recomputes after a backspace or paste`() {
        val formatter = PhoneNumberFormatter.newAsYouTypeFormatter(context, "US")

        PhoneNumberFormatter.formatAsYouType(formatter, "6502530000")
        assertEquals("(650) 253-000", PhoneNumberFormatter.formatAsYouType(formatter, "650253000"))
        assertEquals("+1 650-253-0000", PhoneNumberFormatter.formatAsYouType(formatter, "+16502530000"))
    }

    @Test
    fun `formatAsYouType ignores characters other than digits and plus`() {
        val formatter = PhoneNumberFormatter.newAsYouTypeFormatter(context, "US")

        assertEquals("(650) 253-0000", PhoneNumberFormatter.formatAsYouType(formatter, "650-253 0000"))
    }

    @Test
    fun `formatAsYouType on input with no digits returns it unchanged`() {
        val formatter = PhoneNumberFormatter.newAsYouTypeFormatter(context, "US")

        assertEquals("abc", PhoneNumberFormatter.formatAsYouType(formatter, "abc"))
    }
}
