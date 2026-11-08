package com.sforca.callmanager.core.telephony

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.core.model.ScreeningReason
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

    private fun postedText(): String? =
        shadowOf(manager)
            .allNotifications
            .lastOrNull()
            ?.extras
            ?.getString("android.text")

    @Test
    fun `posts a titled notification and registers its channel`() {
        RejectedCallNotifier.notify(
            context,
            callerName = "Ada",
            number = "+15550100",
            reason = ScreeningReason.BlockList,
        )

        assertEquals("Blocked call from Ada", postedTitle())
        assertEquals("On your block list", postedText())
        assertEquals("Rejected calls", manager.getNotificationChannel("rejected_calls").name.toString())
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `notification text follows the app language`() {
        RejectedCallNotifier.notify(
            context,
            callerName = "Ada",
            number = "+15550100",
            reason = ScreeningReason.BlockList,
        )

        assertEquals("Chamada bloqueada de Ada", postedTitle())
        assertEquals("Na sua lista de bloqueados", postedText())
        assertEquals("Chamadas rejeitadas", manager.getNotificationChannel("rejected_calls").name.toString())
    }

    @Test
    @Config(qualifiers = "es")
    fun `notification text is available in spanish`() {
        RejectedCallNotifier.notify(
            context,
            callerName = "Ada",
            number = "+15550100",
            reason = ScreeningReason.BlockList,
        )

        assertEquals("Llamada bloqueada de Ada", postedTitle())
        assertEquals("En tu lista de bloqueados", postedText())
    }
}
