package com.matheus.callmanager.data

import android.content.Context
import com.matheus.callmanager.util.ContactsUtil
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

sealed class ScreeningDecision {
    data class Allow(val reason: String) : ScreeningDecision()
    data class Reject(val reason: String) : ScreeningDecision()
}

/** Central place where the block-list, contacts and "recently accepted" rules are evaluated. */
class CallRepository(private val context: Context) {
    private val db = CallManagerDatabase.get(context)
    private val blockListDao = db.blockListDao()
    private val acceptedCallerDao = db.acceptedCallerDao()
    private val blockedCallLogDao = db.blockedCallLogDao()
    val settings = SettingsStore(context)

    fun observeBlockList(): Flow<List<BlockListEntry>> = blockListDao.observeAll()
    fun observeBlockedCallLog(): Flow<List<BlockedCallLog>> = blockedCallLogDao.observeAll()

    suspend fun blockNumbers(numbers: List<String>) {
        val now = System.currentTimeMillis()
        blockListDao.insertAll(numbers.map { BlockListEntry(it, now) })
    }

    suspend fun unblock(entry: BlockListEntry) = blockListDao.delete(entry)

    /** Call when the user answers/accepts a call, so the same number is allowed again for X days. */
    suspend fun recordAccepted(number: String) {
        acceptedCallerDao.upsert(AcceptedCaller(number, System.currentTimeMillis()))
    }

    /** Evaluates all configured rules for an incoming [number]. */
    suspend fun evaluate(number: String): ScreeningDecision {
        if (blockListDao.isBlocked(number)) {
            return ScreeningDecision.Reject("On your block list")
        }
        if (settings.allowContacts && ContactsUtil.isContact(context, number)) {
            return ScreeningDecision.Allow("Known contact")
        }
        if (settings.recentAcceptedRuleEnabled) {
            val accepted = acceptedCallerDao.get(number)
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(settings.allowRecentAcceptedDays.toLong())
            if (accepted != null && accepted.lastAcceptedAt >= cutoff) {
                return ScreeningDecision.Allow("Accepted within the last ${settings.allowRecentAcceptedDays} days")
            }
        }
        return if (settings.blockUnknownNumbers) {
            ScreeningDecision.Reject("Unknown number")
        } else {
            ScreeningDecision.Allow("No matching block rule")
        }
    }

    suspend fun logBlockedCall(number: String, reason: String) {
        val name = ContactsUtil.displayName(context, number)
        blockedCallLogDao.insert(
            BlockedCallLog(number = number, callerName = name, timestamp = System.currentTimeMillis(), reason = reason)
        )
    }

    companion object {
        @Volatile private var instance: CallRepository? = null
        fun get(context: Context): CallRepository =
            instance ?: synchronized(this) {
                instance ?: CallRepository(context.applicationContext).also { instance = it }
            }
    }
}
