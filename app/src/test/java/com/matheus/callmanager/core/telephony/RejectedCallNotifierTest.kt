package com.matheus.callmanager.core.telephony

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RejectedCallNotifierTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun postedTitle(): String? =
        shadowOf(manager)
            .allNotifications
            .lastOrNull()
            ?.extras
            ?.getString("android.title")

    @Test
    fun `posts a titled notification and registers its channel`() {
        RejectedCallNotifier.notify(context, callerName = "Ada", number = "+15550100", reason = "On your block list")

        assertEquals("Blocked call from Ada", postedTitle())
        assertEquals("Rejected calls", manager.getNotificationChannel("rejected_calls").name.toString())
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `notification text follows the app language`() {
        RejectedCallNotifier.notify(context, callerName = "Ada", number = "+15550100", reason = "x")

        assertEquals("Chamada bloqueada de Ada", postedTitle())
        assertEquals("Chamadas rejeitadas", manager.getNotificationChannel("rejected_calls").name.toString())
    }

    @Test
    @Config(qualifiers = "es")
    fun `notification text is available in spanish`() {
        RejectedCallNotifier.notify(context, callerName = "Ada", number = "+15550100", reason = "x")

        assertEquals("Llamada bloqueada de Ada", postedTitle())
    }
}
