package com.sforca.callmanager.core.telephony

import android.content.Context
import android.telephony.TelephonyManager
import io.michaelrocks.libphonenumber.android.AsYouTypeFormatter
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

/**
 * Formats phone numbers for display, using Google's libphonenumber so numbers from any country
 * read correctly — not just the device's home country.
 */
object PhoneNumberFormatter {
    @Volatile
    private var phoneUtil: PhoneNumberUtil? = null

    private fun phoneUtil(context: Context): PhoneNumberUtil =
        phoneUtil ?: synchronized(this) {
            phoneUtil ?: PhoneNumberUtil.createInstance(context.applicationContext).also { phoneUtil = it }
        }

    /**
     * Best-guess two-letter region (e.g. "US", "BR") to assume for numbers that don't carry their
     * own country code, in priority order: the SIM's country, the network's country, then the
     * device locale. Falls back to "US" if none of those are available (e.g. no SIM, emulator).
     */
    fun defaultRegion(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simCountry = telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
        val networkCountry = telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
        val localeCountry = Locale.getDefault().country.takeIf { it.isNotBlank() }
        return (simCountry ?: networkCountry ?: localeCountry ?: "US").uppercase(Locale.US)
    }

    /**
     * Formats [rawNumber] for display: numbers that carry a foreign country code (or don't match
     * [defaultRegion]) are shown in full international form (e.g. "+44 20 7946 0958") so they're
     * distinguishable from local numbers; numbers matching [defaultRegion] are shown in the more
     * readable national form (e.g. "(555) 123-4567"). Falls back to [rawNumber] unchanged if it
     * can't be parsed as a phone number (e.g. a short code, or malformed data).
     */
    fun format(
        context: Context,
        rawNumber: String,
        defaultRegion: String,
    ): String {
        val util = phoneUtil(context)
        return try {
            val parsed = util.parse(rawNumber, defaultRegion)
            if (!util.isValidNumber(parsed)) return rawNumber
            val isLocal = parsed.countryCode == util.getCountryCodeForRegion(defaultRegion)
            val format =
                if (isLocal) {
                    PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                } else {
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                }
            util.format(parsed, format)
        } catch (e: NumberParseException) {
            rawNumber
        }
    }

    /**
     * Formats [rawNumber] as "+{country code} (area code)-{subscriber number}" (e.g.
     * "+1 (213)-3734253"), for contexts (like a notification) that want the country code visibly
     * separated from the rest of the number regardless of whether it's "local". Falls back to
     * [format] when the region has no concept of an area code (e.g. many mobile-first countries),
     * and to [rawNumber] unchanged if it doesn't parse as a valid number at all.
     */
    fun formatWithAreaCode(
        context: Context,
        rawNumber: String,
        defaultRegion: String,
    ): String {
        val util = phoneUtil(context)
        return try {
            val parsed = util.parse(rawNumber, defaultRegion)
            if (!util.isValidNumber(parsed)) return rawNumber
            val national = util.getNationalSignificantNumber(parsed)
            val areaCodeLength = util.getLengthOfGeographicalAreaCode(parsed)
            if (areaCodeLength in 1 until national.length) {
                val areaCode = national.substring(0, areaCodeLength)
                val subscriberNumber = national.substring(areaCodeLength)
                "+${parsed.countryCode} ($areaCode)-$subscriberNumber"
            } else {
                "+${parsed.countryCode} $national"
            }
        } catch (e: NumberParseException) {
            rawNumber
        }
    }

    /**
     * Normalizes a manually-typed number for storage/matching: assumes [defaultRegion] (the user's
     * own location) when the number has no country code of its own, so e.g. a Brazilian user typing
     * "19 99999-9999" is stored as "+5519999999999" — matching the E.164 form Telecom hands
     * `CallScreeningService` for real calls. Also tolerates a stray leading zero after '+' (a common
     * typo — dialing prefixes like "00" don't belong after '+', e.g. "+055..." meant "+55...").
     * Falls back to the trimmed input unchanged if it doesn't parse as a valid number at all.
     */
    fun normalizeForStorage(
        context: Context,
        rawNumber: String,
        defaultRegion: String,
    ): String {
        val util = phoneUtil(context)
        val cleaned = stripLeadingZeroAfterPlus(rawNumber.trim())
        return try {
            val parsed = util.parse(cleaned, defaultRegion)
            if (!util.isValidNumber(parsed)) return rawNumber.trim()
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: NumberParseException) {
            rawNumber.trim()
        }
    }

    private fun stripLeadingZeroAfterPlus(number: String): String {
        var result = number
        while (result.startsWith("+0")) {
            result = "+" + result.substring(2)
        }
        return result
    }

    /** A fresh [AsYouTypeFormatter] assuming [defaultRegion], for live formatting while typing. */
    fun newAsYouTypeFormatter(
        context: Context,
        defaultRegion: String,
    ): AsYouTypeFormatter = phoneUtil(context).getAsYouTypeFormatter(defaultRegion)

    /**
     * Re-derives the as-you-type formatting for the *entire* current [input] using [formatter],
     * rather than feeding one new character at a time — this keeps formatting correct after a
     * backspace, paste, or mid-string edit, which a purely incremental `inputDigit` call wouldn't
     * handle. Only digits and a leading '+' are fed in; other characters (spaces, dashes the
     * formatter itself already inserted) are ignored.
     */
    fun formatAsYouType(
        formatter: AsYouTypeFormatter,
        input: String,
    ): String {
        formatter.clear()
        if (input.isEmpty()) return input
        var formatted = input
        for (char in input) {
            if (char.isDigit() || char == '+') {
                formatted = formatter.inputDigit(char)
            }
        }
        return formatted
    }
}
