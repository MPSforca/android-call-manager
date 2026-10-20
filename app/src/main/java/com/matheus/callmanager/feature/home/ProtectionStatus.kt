package com.matheus.callmanager.feature.home

import com.matheus.callmanager.feature.onboarding.PermissionsState

/** Whether the app can screen calls right now. */
enum class ProtectionStatus { ACTIVE, NEEDS_ATTENTION }

/** Battery exemption is optional, so only the screening role and runtime permissions decide it. */
internal fun PermissionsState.protectionStatus(): ProtectionStatus =
    if (coreComplete) ProtectionStatus.ACTIVE else ProtectionStatus.NEEDS_ATTENTION
