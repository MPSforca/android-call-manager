package com.matheus.callmanager.testing

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.robolectric.Shadows

/**
 * Registers `androidx.activity.ComponentActivity` as resolvable so `createComposeRule()` can
 * launch it under Robolectric.
 *
 * Without this, resolution fails under a specific build variant's unit tests: the standard fix
 * (`androidx.compose.ui:ui-test-manifest` as `debugImplementation`) only injects that manifest
 * entry into the *debug* variant's merged manifest, so `testReleaseUnitTest` (which Kover's
 * aggregate report runs alongside `testDebugUnitTest`) fails the same lookup. Registering the
 * component directly in the shadow package manager works identically for every build variant,
 * with no manifest or dependency-configuration scoping involved.
 */
class ComposeHostActivityRule : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = ComponentName(context.packageName, "androidx.activity.ComponentActivity")
        Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(componentName)
    }
}
