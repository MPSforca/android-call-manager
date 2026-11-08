package com.sforca.callmanager.core.designsystem

import android.content.Context
import com.sforca.callmanager.R
import com.sforca.callmanager.core.model.ScreeningReason

/** Localized, user-facing description of a [ScreeningReason]. */
fun ScreeningReason.text(context: Context): String =
    when (this) {
        ScreeningReason.BlockList -> context.getString(R.string.reason_block_list)
        ScreeningReason.AllowList -> context.getString(R.string.reason_allow_list)
        ScreeningReason.NamePattern -> context.getString(R.string.reason_name_pattern)
        ScreeningReason.KnownContact -> context.getString(R.string.reason_known_contact)
        is ScreeningReason.RecentlyAccepted ->
            context.resources.getQuantityString(R.plurals.reason_recently_accepted, days, days)
        is ScreeningReason.RecentlyCalled ->
            context.resources.getQuantityString(R.plurals.reason_recently_called, days, days)
        ScreeningReason.UnknownNumber -> context.getString(R.string.reason_unknown_number)
        ScreeningReason.NoMatchingBlockRule -> context.getString(R.string.reason_no_matching_block_rule)
    }
