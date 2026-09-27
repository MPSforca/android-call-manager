package com.matheus.callmanager.core.telephony

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * The app has no persistent background process: Telecom binds RuleBasedCallScreeningService only
 * when a call actually arrives, and PhoneStateReceiver is a manifest broadcast receiver woken by
 * the system, not a running service. That's already the battery-efficient design — there's nothing
 * to poll and nothing idling.
 *
 * The one real risk is OEM battery managers (Xiaomi/Samsung/Huawei/OnePlus, etc.) that can prevent
 * an app's process from being started at all to service a bound-service call, which would silently
 * break call screening. Asking to be exempted from battery optimization avoids that without running
 * any kind of foreground service or wake lock.
 */
object BatteryUtil {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
}
