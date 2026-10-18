package com.matheus.callmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.feature.lists.ListsViewModel
import com.matheus.callmanager.feature.onboarding.PermissionsViewModel
import com.matheus.callmanager.feature.onboarding.SystemRequests
import com.matheus.callmanager.navigation.SystemActions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionsViewModel: PermissionsViewModel by viewModels()
    private val listsViewModel: ListsViewModel by viewModels()

    private val systemRequests = SystemRequests(this) { permissionsViewModel.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val actions =
            SystemActions(
                requestScreeningRole = systemRequests::requestScreeningRole,
                requestRuntimePermissions = systemRequests::requestRuntimePermissions,
                requestBatteryExemption = systemRequests::requestBatteryExemption,
                pickContactToBlock = { systemRequests.pickContact(listsViewModel::blockContact) },
                pickContactToAllow = { systemRequests.pickContact(listsViewModel::allowContact) },
                skipOnboarding = permissionsViewModel::skip,
            )
        setContent {
            CallManagerTheme {
                Surface {
                    val permissions by permissionsViewModel.state.collectAsStateWithLifecycle()
                    CallManagerApp(permissions = permissions, actions = actions)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionsViewModel.refresh()
    }
}
