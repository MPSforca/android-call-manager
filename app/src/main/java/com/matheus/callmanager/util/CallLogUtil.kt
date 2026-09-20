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
    /** Default page size for [recentCalls] and [systemBlockedCalls] when the caller doesn't override it. */
    const val DEFAULT_PAGE_SIZE = 200

    /**
     * Reads one page from Android's system call log (CallLog.Calls.CONTENT_URI), which is the
     * device's persistent call history — not app-local state. It includes every call regardless
     * of whether this app was running when the call happened.
     *
     * A page shorter than [pageSize] means there are no more calls after it. The LIMIT/OFFSET
     * suffix on [sortOrder] is a standard trick for provider-backed SQLite queries: the framework
     * passes the sort order straight into the underlying `ORDER BY`, which SQLite lets you follow
     * with `LIMIT`/`OFFSET`.
     */
    fun recentCalls(context: Context, pageSize: Int = DEFAULT_PAGE_SIZE, offset: Int = 0): List<RecentCall> {
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
            "${CallLog.Calls.DATE} DESC LIMIT $pageSize OFFSET $offset"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            while (cursor.moveToNext()) {
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
     * Reads one page of calls the system call log tagged as blocked (CallLog.Calls.BLOCKED_TYPE),
     * regardless of who blocked it: this app, Android's own "Blocked numbers" list, the carrier,
     * or another call-screening app. Third-party apps can't enumerate Android's native block list
     * directly (that requires the default-dialer role), but every blocked call still lands here.
     *
     * A page shorter than [pageSize] means there are no more blocked calls after it.
     */
    fun systemBlockedCalls(context: Context, pageSize: Int = DEFAULT_PAGE_SIZE, offset: Int = 0): List<SystemBlockedCall> {
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
            "${CallLog.Calls.DATE} DESC LIMIT $pageSize OFFSET $offset"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val reasonIdx = if (hasBlockReasonColumn) cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON) else -1
            while (cursor.moveToNext()) {
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

    /** Human-readable label for a CallLog.Calls.TYPE value, for showing call outcome in the UI. */
    fun callTypeLabel(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "Answered elsewhere"
        else -> "Call"
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
