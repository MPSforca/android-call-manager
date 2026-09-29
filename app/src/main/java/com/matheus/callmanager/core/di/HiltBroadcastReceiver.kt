package com.matheus.callmanager.core.di

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper

/**
 * Hilt injects `@AndroidEntryPoint` broadcast receivers from an onReceive() override on the
 * generated Hilt_ base class, which only runs if the receiver's own onReceive() calls
 * super.onReceive() first. The plain [BroadcastReceiver.onReceive] is abstract, so Kotlin can't
 * call it via super — this gives every Hilt-injected receiver a concrete no-op to call instead.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) = Unit
}
