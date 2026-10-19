package com.matheus.callmanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.core.datastore.SettingsRepository
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.model.UserPreferences
import com.matheus.callmanager.feature.lists.ListsViewModel
import com.matheus.callmanager.feature.onboarding.PermissionsViewModel
import com.matheus.callmanager.feature.onboarding.SystemRequests
import com.matheus.callmanager.navigation.SystemActions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

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
            val preferences by settings.preferences.collectAsStateWithLifecycle(UserPreferences())
            val darkTheme = preferences.themeMode.isDark(isSystemInDarkTheme())
            // Bar icon colors must follow the chosen mode, not the system one, or forcing Dark on a
            // light-mode phone leaves dark status-bar icons on a dark background.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            CallManagerTheme(darkTheme = darkTheme, dynamicColor = preferences.dynamicColor) {
                Surface {
                    val permissions by permissionsViewModel.state.collectAsStateWithLifecycle()
                    CallManagerApp(permissions = permissions, actions = actions)
                }
            }
        }
    }

    private companion object {
        // The same scrims `enableEdgeToEdge()` uses by default for the navigation bar.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    override fun onResume() {
        super.onResume()
        permissionsViewModel.refresh()
    }
}
