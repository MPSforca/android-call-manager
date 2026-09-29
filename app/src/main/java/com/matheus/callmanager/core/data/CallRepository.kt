package com.matheus.callmanager.core.data

import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.entity.AcceptedCaller
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.database.entity.BlockedCallLog
import com.matheus.callmanager.core.datastore.SettingsStore
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place where the block-list, allow-list, contacts, "recently accepted" and "recently called" rules are evaluated.
 * [database], [contactsLookup] and [recentOutgoingLookup] are injectable so this can be unit tested without a real device.
 */
@Singleton
class CallRepository
    @Inject
    constructor(
        private val database: CallManagerDatabase,
        private val contactsLookup: ContactsLookup,
        private val recentOutgoingLookup: RecentOutgoingLookup,
        val settings: SettingsStore,
    ) {
        private val blockListDao = database.blockListDao()
        private val allowListDao = database.allowListDao()
        private val acceptedCallerDao = database.acceptedCallerDao()
        private val blockedCallLogDao = database.blockedCallLogDao()
        private val allowedNamePatternDao = database.allowedNamePatternDao()

        fun observeBlockList(): Flow<List<BlockListEntry>> = blockListDao.observeAll()

        fun observeAllowList(): Flow<List<AllowListEntry>> = allowListDao.observeAll()

        fun observeBlockedCallLog(): Flow<List<BlockedCallLog>> = blockedCallLogDao.observeAll()

        fun observeAllowedNamePatterns(): Flow<List<AllowedNamePattern>> = allowedNamePatternDao.observeAll()

        suspend fun addAllowedNamePattern(pattern: String) {
            allowedNamePatternDao.insert(AllowedNamePattern(pattern, System.currentTimeMillis()))
        }

        suspend fun removeAllowedNamePattern(entry: AllowedNamePattern) = allowedNamePatternDao.delete(entry)

        suspend fun blockNumbers(numbers: List<Pair<String, String?>>) {
            val now = System.currentTimeMillis()
            blockListDao.insertAll(
                numbers.map { (number, name) ->
                    BlockListEntry(number, name ?: contactsLookup.displayName(number), now)
                },
            )
            // Blocking a number should take precedence over any existing allow entry for it.
            numbers.forEach { (number, _) ->
                allowListDao.getIfExists(number)?.let { allowListDao.delete(it) }
            }
        }

        suspend fun unblock(entry: BlockListEntry) = blockListDao.delete(entry)

        suspend fun allowNumbers(numbers: List<Pair<String, String?>>) {
            val now = System.currentTimeMillis()
            allowListDao.insertAll(
                numbers.map { (number, name) ->
                    AllowListEntry(number, name ?: contactsLookup.displayName(number), now)
                },
            )
            numbers.forEach { (number, _) ->
                blockListDao.getIfExists(number)?.let { blockListDao.delete(it) }
            }
        }

        suspend fun unallow(entry: AllowListEntry) = allowListDao.delete(entry)

        /** Call when the user answers/accepts a call, so the same number is allowed again for X days. */
        suspend fun recordAccepted(number: String) {
            acceptedCallerDao.upsert(AcceptedCaller(number, System.currentTimeMillis()))
        }

        /**
         * Evaluates all configured rules for an incoming [number]. [callerName] is the carrier-supplied
         * caller ID name (CNAM), when Telecom provides one — not always available, so it's nullable.
         */
        suspend fun evaluate(
            number: String,
            callerName: String? = null,
        ): ScreeningDecision {
            if (blockListDao.isBlocked(number)) {
                return ScreeningDecision.Reject("On your block list")
            }
            if (allowListDao.isAllowed(number)) {
                return ScreeningDecision.Allow("On your allow list")
            }
            if (NameAllowMatcher.matches(callerName, allowedNamePatternDao.getAllPatterns())) {
                return ScreeningDecision.Allow("Caller name matches an allow pattern")
            }
            if (settings.allowContacts && contactsLookup.isContact(number)) {
                return ScreeningDecision.Allow("Known contact")
            }
            if (settings.recentAcceptedRuleEnabled) {
                val accepted = acceptedCallerDao.get(number)
                val cutoff =
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(settings.allowRecentAcceptedDays.toLong())
                if (accepted != null && accepted.lastAcceptedAt >= cutoff) {
                    return ScreeningDecision.Allow("Accepted within the last ${settings.allowRecentAcceptedDays} days")
                }
            }
            if (settings.recentOutgoingRuleEnabled) {
                val days = settings.allowRecentOutgoingDays
                val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
                if (recentOutgoingLookup.hasOutgoingCallSince(number, cutoff)) {
                    return ScreeningDecision.Allow("You called them within the last $days days")
                }
            }
            return if (settings.blockUnknownNumbers) {
                ScreeningDecision.Reject("Unknown number")
            } else {
                ScreeningDecision.Allow("No matching block rule")
            }
        }

        /** Returns the caller's contact name, if any, so callers can reuse it (e.g. for a notification). */
        suspend fun logBlockedCall(
            number: String,
            reason: String,
        ): String? {
            val name = contactsLookup.displayName(number)
            blockedCallLogDao.insert(
                BlockedCallLog(
                    number = number,
                    callerName = name,
                    timestamp = System.currentTimeMillis(),
                    reason = reason,
                ),
            )
            return name
        }
    }
