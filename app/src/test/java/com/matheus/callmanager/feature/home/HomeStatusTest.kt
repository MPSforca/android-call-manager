package com.matheus.callmanager.feature.home

import com.matheus.callmanager.feature.onboarding.PermissionsState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusTest {
    @Test
    fun `protection is active once the role and permissions are granted`() {
        val state = PermissionsState(hasRole = true, hasPermissions = true, batteryExempt = true)

        assertEquals(ProtectionStatus.ACTIVE, state.protectionStatus())
    }

    @Test
    fun `protection stays active when only the optional battery step is missing`() {
        val state = PermissionsState(hasRole = true, hasPermissions = true, batteryExempt = false)

        assertEquals(ProtectionStatus.ACTIVE, state.protectionStatus())
    }

    @Test
    fun `protection needs attention when the role is missing`() {
        assertEquals(
            ProtectionStatus.NEEDS_ATTENTION,
            PermissionsState(hasRole = false, hasPermissions = true).protectionStatus(),
        )
    }

    @Test
    fun `protection needs attention when permissions are missing`() {
        assertEquals(
            ProtectionStatus.NEEDS_ATTENTION,
            PermissionsState(hasRole = true, hasPermissions = false).protectionStatus(),
        )
    }
}
