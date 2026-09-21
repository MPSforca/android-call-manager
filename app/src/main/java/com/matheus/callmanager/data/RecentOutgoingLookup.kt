package com.matheus.callmanager.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import com.matheus.callmanager.util.CallLogUtil

/** Abstraction over "did the user call this number recently" so rule evaluation can be unit tested with a fake. */
interface RecentOutgoingLookup {
    fun hasOutgoingCallSince(number: String, sinceMillis: Long): Boolean
}

/**
 * Backed by the system call log, so it also covers calls placed before this app was installed or
 * while it was not running. Numbers are compared with [PhoneNumberUtils.compare] because the call log
 * stores the number as dialed (e.g. "0412 345 678") while Telecom reports incoming ones in E.164.
 */
class AndroidRecentOutgoingLookup(private val context: Context) : RecentOutgoingLookup {
    // compare() is deprecated, but its replacement (areSamePhoneNumber) is API 31+ and minSdk is lower.
    @Suppress("DEPRECATION")
    override fun hasOutgoingCallSince(number: String, sinceMillis: Long): Boolean = try {
        CallLogUtil.outgoingNumbersSince(context, sinceMillis).any { PhoneNumberUtils.compare(it, number) }
    } catch (e: SecurityException) {
        // READ_CALL_LOG revoked: fail closed (rule simply doesn't match) rather than crash call screening.
        false
    }
}
