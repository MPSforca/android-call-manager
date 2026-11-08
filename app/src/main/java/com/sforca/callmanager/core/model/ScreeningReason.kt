package com.sforca.callmanager.core.model

/**
 * Why a call was allowed or rejected. Kept as data (not display text) so the UI can localize it;
 * [code] is the stable value persisted in the blocked-call log.
 */
sealed interface ScreeningReason {
    val code: String

    data object BlockList : ScreeningReason {
        override val code = "block_list"
    }

    data object AllowList : ScreeningReason {
        override val code = "allow_list"
    }

    data object NamePattern : ScreeningReason {
        override val code = "name_pattern"
    }

    data object KnownContact : ScreeningReason {
        override val code = "known_contact"
    }

    data class RecentlyAccepted(
        val days: Int,
    ) : ScreeningReason {
        override val code = "recently_accepted"
    }

    data class RecentlyCalled(
        val days: Int,
    ) : ScreeningReason {
        override val code = "recently_called"
    }

    data object UnknownNumber : ScreeningReason {
        override val code = "unknown_number"
    }

    data object NoMatchingBlockRule : ScreeningReason {
        override val code = "no_matching_block_rule"
    }
}
