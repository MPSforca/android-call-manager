package com.sforca.callmanager.testing

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf

/**
 * Registers `ComponentActivity` with Robolectric so `createComposeRule()` can launch it in every
 * variant, without pulling `ui-test-manifest` (which only merges into the debug manifest) into the
 * release unit-test run that Kover's report also triggers.
 */
class ComposeHostRule : ExternalResource() {
    override fun before() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        shadowOf(app.packageManager)
            .addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
    }
}
