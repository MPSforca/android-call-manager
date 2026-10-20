package com.sforca.callmanager.core.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sforca.callmanager.R

/**
 * Posts a heads-up-free notification when a call gets fully rejected, since those otherwise vanish
 * from the incoming-call UI without a trace until the user checks the Blocked tab (see
 * ScreeningPolicy.shouldNotifyRejected). Not unit tested — like AndroidContactsLookup, this is a
 * thin wrapper around a real Android system service; verified manually/via adb (see README).
 */
object RejectedCallNotifier {
    private const val CHANNEL_ID = "rejected_calls"
    private var notificationId = 0

    /** [callerName] should be the caller's contact/caller-ID name, when known; falls back to a formatted [number]. */
    fun notify(
        context: Context,
        callerName: String?,
        number: String,
        reason: String,
    ) {
        ensureChannel(context)
        val title =
            callerName?.takeIf { it.isNotBlank() }
                ?: PhoneNumberFormatter.formatWithAreaCode(context, number, PhoneNumberFormatter.defaultRegion(context))
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_blocked_title, title))
                .setContentText(reason)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(notificationId++, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
        manager.createNotificationChannel(channel)
    }
}
