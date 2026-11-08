package com.sforca.callmanager.core.model

sealed class ScreeningDecision {
    abstract val reason: ScreeningReason

    data class Allow(
        override val reason: ScreeningReason,
    ) : ScreeningDecision()

    data class Reject(
        override val reason: ScreeningReason,
    ) : ScreeningDecision()
}
