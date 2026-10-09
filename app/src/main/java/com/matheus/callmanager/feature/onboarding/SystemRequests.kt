package com.matheus.callmanager.feature.onboarding

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.matheus.callmanager.core.telephony.BatteryUtil
import com.matheus.callmanager.core.telephony.ContactsUtil

/**
 * Owns the activity-result launchers (role, runtime permissions, battery exemption, contact
 * picker) so `MainActivity` doesn't. Must be constructed during activity initialization, before
 * the activity is STARTED, because it registers launchers.
 *
 * [onSystemStateChanged] fires after any system dialog returns so callers can re-read permission
 * state.
 */
class SystemRequests(
    private val activity: ComponentActivity,
    private val onSystemStateChanged: () -> Unit,
) {
    private var pendingContactPick: ((number: String, name: String?) -> Unit)? = null

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onSystemStateChanged()
        }

    private val roleLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onSystemStateChanged()
        }

    private val batteryLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onSystemStateChanged()
        }

    private val contactPickLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            val callback = pendingContactPick
            pendingContactPick = null
            if (uri != null && callback != null) {
                ContactsUtil.resolvePickedPhoneNumber(activity, uri)?.let { (number, name) -> callback(number, name) }
            }
        }

    fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = activity.getSystemService(RoleManager::class.java) ?: return
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    fun requestRuntimePermissions() {
        permissionLauncher.launch(requiredRuntimePermissions().toTypedArray())
    }

    fun requestBatteryExemption() {
        batteryLauncher.launch(BatteryUtil.requestIgnoreBatteryOptimizationsIntent(activity))
    }

    fun pickContact(onPicked: (number: String, name: String?) -> Unit) {
        pendingContactPick = onPicked
        contactPickLauncher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
    }
}
