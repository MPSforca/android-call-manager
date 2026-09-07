package com.matheus.callmanager.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

sealed class ScreeningDecision {
    data class Allow(val reason: String) : ScreeningDecision()
    data class Reject(val reason: String) : ScreeningDecision()
}

/**
 * Central place where the block-list, allow-list, contacts and "recently accepted" rules are evaluated.
 * [database] and [contactsLookup] are injectable so this can be unit tested without a real device.
 */
class CallRepository(
    context: Context,
    private val database: CallManagerDatabase = CallManagerDatabase.get(context),
    private val contactsLookup: ContactsLookup = AndroidContactsLookup(context)
) {
    private val blockListDao = database.blockListDao()
    private val allowListDao = database.allowListDao()
    private val acceptedCallerDao = database.acceptedCallerDao()
    private val blockedCallLogDao = database.blockedCallLogDao()
    val settings = SettingsStore(context)

    fun observeBlockList(): Flow<List<BlockListEntry>> = blockListDao.observeAll()
    fun observeAllowList(): Flow<List<AllowListEntry>> = allowListDao.observeAll()
    fun observeBlockedCallLog(): Flow<List<BlockedCallLog>> = blockedCallLogDao.observeAll()

    suspend fun blockNumbers(numbers: List<Pair<String, String?>>) {
        val now = System.currentTimeMillis()
        blockListDao.insertAll(numbers.map { (number, name) ->
            BlockListEntry(number, name ?: contactsLookup.displayName(number), now)
        })
        // Blocking a number should take precedence over any existing allow entry for it.
        numbers.forEach { (number, _) ->
            allowListDao.getIfExists(number)?.let { allowListDao.delete(it) }
        }
    }

    suspend fun unblock(entry: BlockListEntry) = blockListDao.delete(entry)

    suspend fun allowNumbers(numbers: List<Pair<String, String?>>) {
        val now = System.currentTimeMillis()
        allowListDao.insertAll(numbers.map { (number, name) ->
            AllowListEntry(number, name ?: contactsLookup.displayName(number), now)
        })
        numbers.forEach { (number, _) ->
            blockListDao.getIfExists(number)?.let { blockListDao.delete(it) }
        }
    }

    suspend fun unallow(entry: AllowListEntry) = allowListDao.delete(entry)

    /** Call when the user answers/accepts a call, so the same number is allowed again for X days. */
    suspend fun recordAccepted(number: String) {
        acceptedCallerDao.upsert(AcceptedCaller(number, System.currentTimeMillis()))
    }

    /** Evaluates all configured rules for an incoming [number]. */
    suspend fun evaluate(number: String): ScreeningDecision {
        if (blockListDao.isBlocked(number)) {
            return ScreeningDecision.Reject("On your block list")
        }
        if (allowListDao.isAllowed(number)) {
            return ScreeningDecision.Allow("On your allow list")
        }
        if (settings.allowContacts && contactsLookup.isContact(number)) {
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
        val name = contactsLookup.displayName(number)
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
