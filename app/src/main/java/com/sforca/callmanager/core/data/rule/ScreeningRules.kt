package com.sforca.callmanager.core.data.rule

import com.sforca.callmanager.core.data.NameAllowMatcher
import com.sforca.callmanager.core.database.dao.AcceptedCallerDao
import com.sforca.callmanager.core.database.dao.AllowListDao
import com.sforca.callmanager.core.database.dao.AllowedNamePatternDao
import com.sforca.callmanager.core.database.dao.BlockListDao
import com.sforca.callmanager.core.model.ScreeningDecision
import com.sforca.callmanager.core.model.ScreeningReason
import com.sforca.callmanager.core.telephony.ContactsLookup
import com.sforca.callmanager.core.telephony.RecentOutgoingLookup
import java.util.concurrent.TimeUnit

class BlockListRule(
    private val dao: BlockListDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (dao.isBlocked(context.number)) ScreeningDecision.Reject(ScreeningReason.BlockList) else null
}

class AllowListRule(
    private val dao: AllowListDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (dao.isAllowed(context.number)) ScreeningDecision.Allow(ScreeningReason.AllowList) else null
}

class NamePatternRule(
    private val dao: AllowedNamePatternDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (NameAllowMatcher.matches(context.callerName, dao.getAllPatterns())) {
            ScreeningDecision.Allow(ScreeningReason.NamePattern)
        } else {
            null
        }
}

class ContactRule(
    private val contactsLookup: ContactsLookup,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (context.preferences.allowContacts && contactsLookup.isContact(context.number)) {
            ScreeningDecision.Allow(ScreeningReason.KnownContact)
        } else {
            null
        }
}

class RecentlyAcceptedRule(
    private val dao: AcceptedCallerDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? {
        if (!context.preferences.recentAcceptedRuleEnabled) return null
        val days = context.preferences.allowRecentAcceptedDays
        val cutoff = context.nowMillis - TimeUnit.DAYS.toMillis(days.toLong())
        val accepted = dao.get(context.number)
        return if (accepted != null && accepted.lastAcceptedAt >= cutoff) {
            ScreeningDecision.Allow(ScreeningReason.RecentlyAccepted(days))
        } else {
            null
        }
    }
}

class RecentlyCalledRule(
    private val lookup: RecentOutgoingLookup,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? {
        if (!context.preferences.recentOutgoingRuleEnabled) return null
        val days = context.preferences.allowRecentOutgoingDays
        val cutoff = context.nowMillis - TimeUnit.DAYS.toMillis(days.toLong())
        return if (lookup.hasOutgoingCallSince(context.number, cutoff)) {
            ScreeningDecision.Allow(ScreeningReason.RecentlyCalled(days))
        } else {
            null
        }
    }
}

/** Terminal rule: always decides, so the chain can never fall through without a verdict. */
class UnknownNumberFallbackRule : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision =
        if (context.preferences.blockUnknownNumbers) {
            ScreeningDecision.Reject(ScreeningReason.UnknownNumber)
        } else {
            ScreeningDecision.Allow(ScreeningReason.NoMatchingBlockRule)
        }
}
