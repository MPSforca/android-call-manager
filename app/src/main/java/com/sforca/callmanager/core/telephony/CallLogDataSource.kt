package com.sforca.callmanager.core.telephony

import android.content.Context
import com.sforca.callmanager.core.model.RecentCall
import com.sforca.callmanager.core.model.SystemBlockedCall
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Abstraction over [CallLogUtil]'s paged system call log queries so ViewModels can be unit tested with a fake. */
interface CallLogDataSource {
    fun recentCalls(offset: Int): List<RecentCall>

    fun systemBlockedCalls(offset: Int): List<SystemBlockedCall>
}

class AndroidCallLogDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CallLogDataSource {
        override fun recentCalls(offset: Int): List<RecentCall> = CallLogUtil.recentCalls(context, offset = offset)

        override fun systemBlockedCalls(offset: Int): List<SystemBlockedCall> =
            CallLogUtil.systemBlockedCalls(context, offset = offset)
    }
