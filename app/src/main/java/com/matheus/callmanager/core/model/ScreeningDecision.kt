package com.matheus.callmanager.core.model

sealed class ScreeningDecision {
    data class Allow(
        val reason: String,
    ) : ScreeningDecision()

    data class Reject(
        val reason: String,
    ) : ScreeningDecision()
}
