package com.matheus.callmanager.core.model

data class RecentCall(
    val number: String,
    val name: String?,
    val timestamp: Long,
    val type: Int,
)
