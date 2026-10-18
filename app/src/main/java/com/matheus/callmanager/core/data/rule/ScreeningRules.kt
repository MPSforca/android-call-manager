package com.matheus.callmanager.core.data.rule

import com.matheus.callmanager.core.data.NameAllowMatcher
import com.matheus.callmanager.core.database.dao.AcceptedCallerDao
import com.matheus.callmanager.core.database.dao.AllowListDao
import com.matheus.callmanager.core.database.dao.AllowedNamePatternDao
import com.matheus.callmanager.core.database.dao.BlockListDao
import com.matheus.callmanager.core.model.ScreeningDecision
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import java.util.concurrent.TimeUnit

class BlockListRule(
    private val dao: BlockListDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (dao.isBlocked(context.number)) ScreeningDecision.Reject("On your block list") else null
}

class AllowListRule(
    private val dao: AllowListDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (dao.isAllowed(context.number)) ScreeningDecision.Allow("On your allow list") else null
}

class NamePatternRule(
    private val dao: AllowedNamePatternDao,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (NameAllowMatcher.matches(context.callerName, dao.getAllPatterns())) {
            ScreeningDecision.Allow("Caller name matches an allow pattern")
        } else {
            null
        }
}

class ContactRule(
    private val contactsLookup: ContactsLookup,
) : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision? =
        if (context.preferences.allowContacts && contactsLookup.isContact(context.number)) {
            ScreeningDecision.Allow("Known contact")
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
            ScreeningDecision.Allow("Accepted within the last $days days")
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
            ScreeningDecision.Allow("You called them within the last $days days")
        } else {
            null
        }
    }
}

/** Terminal rule: always decides, so the chain can never fall through without a verdict. */
class UnknownNumberFallbackRule : ScreeningRule {
    override suspend fun evaluate(context: ScreeningContext): ScreeningDecision =
        if (context.preferences.blockUnknownNumbers) {
            ScreeningDecision.Reject("Unknown number")
        } else {
            ScreeningDecision.Allow("No matching block rule")
        }
}
