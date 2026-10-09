package com.matheus.callmanager.testing

import com.matheus.callmanager.core.model.RecentCall
import com.matheus.callmanager.core.model.SystemBlockedCall
import com.matheus.callmanager.core.telephony.CallLogDataSource
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle

class FakeContactsLookup : ContactsLookup {
    val contacts = mutableMapOf<String, String>()

    override fun isContact(number: String): Boolean = contacts.containsKey(number)

    override fun displayName(number: String): String? = contacts[number]
}

class FakeRecentOutgoingLookup : RecentOutgoingLookup {
    /** number -> timestamp of the most recent outgoing call to it. */
    val outgoingCalls = mutableMapOf<String, Long>()

    override fun hasOutgoingCallSince(
        number: String,
        sinceMillis: Long,
    ): Boolean = (outgoingCalls[number] ?: Long.MIN_VALUE) >= sinceMillis
}

/** Returns one queued page per call (by order, ignoring [offset]); empty once the queue is exhausted. */
class FakeCallLogDataSource : CallLogDataSource {
    var recentCallPages: List<List<RecentCall>> = emptyList()
    var blockedCallPages: List<List<SystemBlockedCall>> = emptyList()
    var recentCallsCallCount = 0
    var systemBlockedCallsCallCount = 0

    override fun recentCalls(offset: Int): List<RecentCall> {
        val page = recentCallPages.getOrElse(recentCallsCallCount) { emptyList() }
        recentCallsCallCount++
        return page
    }

    override fun systemBlockedCalls(offset: Int): List<SystemBlockedCall> {
        val page = blockedCallPages.getOrElse(systemBlockedCallsCallCount) { emptyList() }
        systemBlockedCallsCallCount++
        return page
    }
}

class FakeActivityRetainedLifecycle : ActivityRetainedLifecycle {
    private val listeners = mutableListOf<RetainedLifecycle.OnClearedListener>()

    override fun addOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
        listeners += listener
    }

    override fun removeOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
        listeners -= listener
    }

    fun triggerCleared() = listeners.forEach { it.onCleared() }
}
