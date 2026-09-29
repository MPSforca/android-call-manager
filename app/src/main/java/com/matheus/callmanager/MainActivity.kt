package com.matheus.callmanager

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.telephony.BatteryUtil
import com.matheus.callmanager.core.telephony.ContactsUtil
import com.matheus.callmanager.feature.lists.ListsViewModel
import com.matheus.callmanager.feature.onboarding.RequestRoleScreen
import com.matheus.callmanager.feature.recent.RecentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val recentViewModel: RecentViewModel by viewModels()
    private val listsViewModel: ListsViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { recentViewModel.refreshRecentCalls() }

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // No-op: user accepted or declined the call-screening role request.
        }

    private var batteryOptimizationIgnored by mutableStateOf(false)

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            batteryOptimizationIgnored = BatteryUtil.isIgnoringBatteryOptimizations(this)
        }

    private var pendingContactPickTarget: ContactPickTarget? = null

    private val contactPickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            val target = pendingContactPickTarget
            pendingContactPickTarget = null
            if (uri != null && target != null) {
                ContactsUtil.resolvePickedPhoneNumber(this, uri)?.let { (number, name) ->
                    when (target) {
                        ContactPickTarget.BLOCK -> listsViewModel.blockContact(number, name)
                        ContactPickTarget.ALLOW -> listsViewModel.allowContact(number, name)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()
        setContent {
            CallManagerTheme {
                Surface {
                    var roleRequested by remember { mutableStateOf(false) }
                    if (!hasScreeningRole() && !roleRequested) {
                        RequestRoleScreen(onRequest = {
                            roleRequested = true
                            requestScreeningRole()
                        })
                    } else {
                        CallManagerApp(
                            onPickContactToBlock = { pickContact(ContactPickTarget.BLOCK) },
                            onPickContactToAllow = { pickContact(ContactPickTarget.ALLOW) },
                            isIgnoringBatteryOptimizations = batteryOptimizationIgnored,
                            onRequestIgnoreBatteryOptimizations = {
                                batteryOptimizationLauncher.launch(
                                    BatteryUtil.requestIgnoreBatteryOptimizationsIntent(this),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    private fun pickContact(target: ContactPickTarget) {
        pendingContactPickTarget = target
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickLauncher.launch(intent)
    }

    private enum class ContactPickTarget { BLOCK, ALLOW }

    override fun onResume() {
        super.onResume()
        recentViewModel.refreshRecentCalls()
        recentViewModel.refreshBlockedCalls()
        batteryOptimizationIgnored = BatteryUtil.isIgnoringBatteryOptimizations(this)
    }

    private fun requestNeededPermissions() {
        val needed =
            mutableListOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing =
            needed.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasScreeningRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }
}
