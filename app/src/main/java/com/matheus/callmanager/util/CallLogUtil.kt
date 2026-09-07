package com.matheus.callmanager.util

import android.content.Context
import android.os.Build
import android.provider.CallLog

data class RecentCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    val type: Int
)

/** One row in the Recent tab: every [RecentCall] from the same number, newest first. */
data class RecentCallGroup(
    val number: String,
    val name: String?,
    val calls: List<RecentCall>
) {
    val latestTimestamp: Long get() = calls.first().timestamp
}

data class SystemBlockedCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    /** Human-readable source, derived from CallLog.Calls.BLOCK_REASON where available (API 29+). */
    val source: String
)

object CallLogUtil {
    /**
     * Reads from Android's system call log (CallLog.Calls.CONTENT_URI), which is the device's
     * persistent call history — not app-local state. It includes every call regardless of
     * whether this app was running when the call happened.
     */
    fun recentCalls(context: Context, limit: Int = 500): List<RecentCall> {
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE
        )
        val results = mutableListOf<RecentCall>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            while (cursor.moveToNext() && results.size < limit) {
                results += RecentCall(
                    number = cursor.getString(numberIdx) ?: continue,
                    name = cursor.getString(nameIdx),
                    timestamp = cursor.getLong(dateIdx),
                    type = cursor.getInt(typeIdx)
                )
            }
        }
        return results
    }

    /**
     * Every call the system call log tagged as blocked (CallLog.Calls.BLOCKED_TYPE), regardless of
     * who blocked it: this app, Android's own "Blocked numbers" list, the carrier, or another
     * call-screening app. Third-party apps can't enumerate Android's native block list directly
     * (that requires the default-dialer role), but every blocked call still lands here.
     */
    fun systemBlockedCalls(context: Context, limit: Int = 500): List<SystemBlockedCall> {
        val hasBlockReasonColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val projection = buildList {
            add(CallLog.Calls.NUMBER)
            add(CallLog.Calls.CACHED_NAME)
            add(CallLog.Calls.DATE)
            if (hasBlockReasonColumn) add(CallLog.Calls.BLOCK_REASON)
        }.toTypedArray()

        val results = mutableListOf<SystemBlockedCall>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.BLOCKED_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val reasonIdx = if (hasBlockReasonColumn) cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON) else -1
            while (cursor.moveToNext() && results.size < limit) {
                val reason = if (reasonIdx >= 0) blockReasonLabel(cursor.getInt(reasonIdx)) else "Blocked by Android"
                results += SystemBlockedCall(
                    number = cursor.getString(numberIdx) ?: continue,
                    name = cursor.getString(nameIdx),
                    timestamp = cursor.getLong(dateIdx),
                    source = reason
                )
            }
        }
        return results
    }

    /** Groups calls by number (newest call per group first, groups ordered by their newest call). */
    fun groupByNumber(calls: List<RecentCall>): List<RecentCallGroup> {
        return calls
            .groupBy { it.number }
            .map { (number, callsForNumber) ->
                val sorted = callsForNumber.sortedByDescending { it.timestamp }
                RecentCallGroup(
                    number = number,
                    name = sorted.firstNotNullOfOrNull { it.name },
                    calls = sorted
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    private fun blockReasonLabel(reason: Int): String = when (reason) {
        CallLog.Calls.BLOCK_REASON_BLOCKED_NUMBER -> "Android's block list"
        CallLog.Calls.BLOCK_REASON_DIRECT_TO_VOICEMAIL -> "Sent to voicemail"
        CallLog.Calls.BLOCK_REASON_CALL_SCREENING_SERVICE -> "A call-screening app"
        CallLog.Calls.BLOCK_REASON_UNKNOWN_NUMBER -> "Unknown-number blocking"
        CallLog.Calls.BLOCK_REASON_NOT_BLOCKED -> "Not blocked"
        else -> "Blocked by Android"
    }
}
