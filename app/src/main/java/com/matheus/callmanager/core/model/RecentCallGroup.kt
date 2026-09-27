package com.matheus.callmanager.core.model

/** One row in the Recent tab: every [RecentCall] from the same number, newest first. */
data class RecentCallGroup(
    val number: String,
    val name: String?,
    val calls: List<RecentCall>,
) {
    val latestTimestamp: Long get() = calls.first().timestamp
}
