package com.sforca.callmanager.core.di

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HiltBroadcastReceiverTest {
    private class NoOpReceiver : HiltBroadcastReceiver()

    @Test
    fun `onReceive is a no-op that does not throw`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        NoOpReceiver().onReceive(context, Intent("test.action"))
    }
}
