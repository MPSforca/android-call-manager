package com.sforca.callmanager.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSystemPermissionStatus(
    var role: Boolean = false,
    var permissions: Boolean = false,
    var battery: Boolean = false,
) : SystemPermissionStatus {
    override fun hasScreeningRole() = role

    override fun hasRuntimePermissions() = permissions

    override fun isIgnoringBatteryOptimizations() = battery
}

class PermissionsViewModelTest {
    private val status = FakeSystemPermissionStatus()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) = PermissionsViewModel(status, saved)

    @Test
    fun `steps are reported in order role then permissions then battery`() {
        val vm = viewModel()
        assertEquals(OnboardingStep.ROLE, vm.state.value.nextStep)

        status.role = true
        vm.refresh()
        assertEquals(OnboardingStep.PERMISSIONS, vm.state.value.nextStep)

        status.permissions = true
        vm.refresh()
        assertEquals(OnboardingStep.BATTERY, vm.state.value.nextStep)
        assertTrue(vm.state.value.coreComplete)

        status.battery = true
        vm.refresh()
        assertNull(vm.state.value.nextStep)
    }

    @Test
    fun `isDone reflects each step`() {
        status.permissions = true
        val state = viewModel().state.value

        assertFalse(state.isDone(OnboardingStep.ROLE))
        assertTrue(state.isDone(OnboardingStep.PERMISSIONS))
        assertFalse(state.isDone(OnboardingStep.BATTERY))
    }

    @Test
    fun `onboarding is needed until core steps are done or skipped`() {
        val vm = viewModel()
        assertTrue(vm.state.value.needsOnboarding)

        vm.skip()

        assertFalse(vm.state.value.needsOnboarding)
        assertTrue(vm.state.value.showBanner)
    }

    @Test
    fun `battery exemption alone is not required to leave onboarding`() {
        status.role = true
        status.permissions = true
        val state = viewModel().state.value

        assertFalse(state.needsOnboarding)
        assertFalse(state.showBanner)
    }

    @Test
    fun `skip survives recreation through saved state`() {
        val saved = SavedStateHandle()
        viewModel(saved).skip()

        assertTrue(viewModel(saved).state.value.skipped)
    }

    @Test
    fun `a revoked permission brings the banner back`() {
        status.role = true
        status.permissions = true
        val vm = viewModel()
        assertFalse(vm.state.value.showBanner)

        status.permissions = false
        vm.refresh()

        assertTrue(vm.state.value.showBanner)
    }
}
