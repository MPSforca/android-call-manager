package com.sforca.callmanager.navigation

/** Actions that need the hosting activity (system dialogs); supplied by `MainActivity`. */
class SystemActions(
    val requestScreeningRole: () -> Unit,
    val requestRuntimePermissions: () -> Unit,
    val requestBatteryExemption: () -> Unit,
    val pickContactToBlock: () -> Unit,
    val pickContactToAllow: () -> Unit,
    val skipOnboarding: () -> Unit,
)
