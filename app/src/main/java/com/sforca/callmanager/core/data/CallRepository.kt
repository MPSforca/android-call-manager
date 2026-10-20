package com.sforca.callmanager.core.data

import com.sforca.callmanager.core.data.rule.AllowListRule
import com.sforca.callmanager.core.data.rule.BlockListRule
import com.sforca.callmanager.core.data.rule.ContactRule
import com.sforca.callmanager.core.data.rule.NamePatternRule
import com.sforca.callmanager.core.data.rule.RecentlyAcceptedRule
import com.sforca.callmanager.core.data.rule.RecentlyCalledRule
import com.sforca.callmanager.core.data.rule.ScreeningContext
import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.core.data.rule.ScreeningTrace
import com.sforca.callmanager.core.data.rule.TraceOutcome
import com.sforca.callmanager.core.data.rule.TraceStep
import com.sforca.callmanager.core.data.rule.UnknownNumberFallbackRule
import com.sforca.callmanager.core.database.CallManagerDatabase
import com.sforca.callmanager.core.database.entity.AcceptedCaller
import com.sforca.callmanager.core.database.entity.AllowListEntry
import com.sforca.callmanager.core.database.entity.AllowedNamePattern
import com.sforca.callmanager.core.database.entity.BlockListEntry
import com.sforca.callmanager.core.database.entity.BlockedCallLog
import com.sforca.callmanager.core.datastore.SettingsRepository
import com.sforca.callmanager.core.model.ScreeningDecision
import com.sforca.callmanager.core.telephony.ContactsLookup
import com.sforca.callmanager.core.telephony.RecentOutgoingLookup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val settings: SettingsRepository,
    ) {
        private val blockListDao = database.blockListDao()
        private val allowListDao = database.allowListDao()
        private val acceptedCallerDao = database.acceptedCallerDao()
        private val blockedCallLogDao = database.blockedCallLogDao()
        private val allowedNamePatternDao = database.allowedNamePatternDao()

        /** Rule chain in precedence order: the first rule to return a decision wins. */
        private val screeningRules =
            listOf(
                ScreeningRuleId.BLOCK_LIST to BlockListRule(blockListDao),
                ScreeningRuleId.ALLOW_LIST to AllowListRule(allowListDao),
                ScreeningRuleId.NAME_PATTERN to NamePatternRule(allowedNamePatternDao),
                ScreeningRuleId.CONTACT to ContactRule(contactsLookup),
                ScreeningRuleId.RECENTLY_ACCEPTED to RecentlyAcceptedRule(acceptedCallerDao),
                ScreeningRuleId.RECENTLY_CALLED to RecentlyCalledRule(recentOutgoingLookup),
                ScreeningRuleId.UNKNOWN_FALLBACK to UnknownNumberFallbackRule(),
            )

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
        ): ScreeningDecision = evaluateWithTrace(number, callerName).decision

        /** Same chain as [evaluate], but also reports which rules ran, which deferred and which decided. */
        suspend fun evaluateWithTrace(
            number: String,
            callerName: String? = null,
        ): ScreeningTrace {
            val context = ScreeningContext(number, callerName, settings.preferences.first(), System.currentTimeMillis())
            var decision: ScreeningDecision? = null
            val steps =
                screeningRules.map { (id, rule) ->
                    if (decision != null) return@map TraceStep(id, TraceOutcome.NOT_REACHED)
                    decision = rule.evaluate(context)
                    TraceStep(id, if (decision != null) TraceOutcome.DECIDED else TraceOutcome.PASSED)
                }
            return ScreeningTrace(checkNotNull(decision) { "Rule chain must end with a deciding rule" }, steps)
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
