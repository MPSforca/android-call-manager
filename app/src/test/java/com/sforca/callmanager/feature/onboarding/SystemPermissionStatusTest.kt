package com.sforca.callmanager.feature.onboarding

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemPermissionStatusTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val status = AndroidSystemPermissionStatus(app)

    @Test
    fun `notifications permission is only required on API 33 and up`() {
        assertFalse(Manifest.permission.POST_NOTIFICATIONS in requiredRuntimePermissions(sdkInt = 32))
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in requiredRuntimePermissions(sdkInt = 33))
    }

    @Test
    fun `runtime permissions are reported only when every one is granted`() {
        assertFalse(status.hasRuntimePermissions())

        shadowOf(app).grantPermissions(*requiredRuntimePermissions().toTypedArray())

        assertTrue(status.hasRuntimePermissions())
    }

    @Test
    fun `screening role is not held by default`() {
        assertFalse(status.hasScreeningRole())
    }

    @Test
    fun `battery optimization state is read from the power manager`() {
        // Robolectric's PowerManager reports not-ignoring unless told otherwise.
        assertEquals(false, status.isIgnoringBatteryOptimizations())
    }
}
