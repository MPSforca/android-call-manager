package com.matheus.callmanager.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.matheus.callmanager.core.telephony.BatteryUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Read-only view of the system state the app depends on. Behind an interface for unit testing. */
interface SystemPermissionStatus {
    fun hasScreeningRole(): Boolean

    fun hasRuntimePermissions(): Boolean

    fun isIgnoringBatteryOptimizations(): Boolean
}

/** Runtime permissions the app asks for (POST_NOTIFICATIONS only exists on API 33+). */
@SuppressLint("InlinedApi") // POST_NOTIFICATIONS is only added when sdkInt >= TIRAMISU.
internal fun requiredRuntimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
    val permissions =
        mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
        )
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) permissions += Manifest.permission.POST_NOTIFICATIONS
    return permissions
}

class AndroidSystemPermissionStatus
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SystemPermissionStatus {
        override fun hasScreeningRole(): Boolean {
            // The role (and RoleManager) only exist on API 29+; older devices have nothing to request.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }

        override fun hasRuntimePermissions(): Boolean =
            requiredRuntimePermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

        override fun isIgnoringBatteryOptimizations(): Boolean = BatteryUtil.isIgnoringBatteryOptimizations(context)
    }
