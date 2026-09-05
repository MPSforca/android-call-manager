package com.matheus.callmanager.util

import android.content.Context
import android.provider.CallLog

data class RecentCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    val type: Int
)

object CallLogUtil {
    fun recentCalls(context: Context, limit: Int = 100): List<RecentCall> {
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
}
