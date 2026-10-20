package com.matheus.callmanager.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matheus.callmanager.BuildConfig
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CmButton
import com.matheus.callmanager.core.designsystem.component.CmCard
import com.matheus.callmanager.core.designsystem.component.CmSegmentedButton
import com.matheus.callmanager.core.designsystem.component.CmSwitchRow
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.core.model.ThemeMode
import com.matheus.callmanager.core.model.UserPreferences

internal const val PRIVACY_URL = "https://github.com/MPSforca/android-call-manager/blob/main/PRIVACY.md"
internal const val LICENSE_URL = "https://github.com/MPSforca/android-call-manager/blob/main/LICENSE"

/** App-wide settings: appearance, protection against the system killing screening, and about. */
@Composable
fun SettingsScreen(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    SettingsContent(
        preferences = preferences,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        versionName = BuildConfig.VERSION_NAME,
        actions =
            SettingsActions(
                onThemeModeChange = viewModel::setThemeMode,
                onDynamicColorChange = viewModel::setDynamicColor,
                onRequestIgnoreBatteryOptimizations = onRequestIgnoreBatteryOptimizations,
                onOpenUrl = uriHandler::openUri,
                onOpenPermissions = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ),
    )
}

internal data class SettingsActions(
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onRequestIgnoreBatteryOptimizations: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onOpenPermissions: () -> Unit,
)

/** Stateless body of [SettingsScreen]. */
@Composable
internal fun SettingsContent(
    preferences: UserPreferences,
    isIgnoringBatteryOptimizations: Boolean,
    dynamicColorSupported: Boolean,
    versionName: String,
    actions: SettingsActions,
) {
    val spacing = LocalSpacing.current
    val themeLabels = ThemeMode.entries.associateWith { stringResource(it.labelRes()) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.medium)) {
        SectionHeader(title = stringResource(R.string.settings_section_appearance))
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
        )
        CmSegmentedButton(
            options = ThemeMode.entries,
            selected = preferences.themeMode,
            onSelect = actions.onThemeModeChange,
            label = { themeLabels.getValue(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
        )
        if (dynamicColorSupported) {
            CmSwitchRow(
                title = stringResource(R.string.settings_dynamic_color_title),
                subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                checked = preferences.dynamicColor,
                onCheckedChange = actions.onDynamicColorChange,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_protection))
        if (isIgnoringBatteryOptimizations) {
            Text(
                text = stringResource(R.string.settings_battery_ok),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
            )
        } else {
            BatteryCard(actions.onRequestIgnoreBatteryOptimizations)
        }

        SectionHeader(title = stringResource(R.string.settings_section_about))
        Text(
            text = stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
        )
        LinkRow(stringResource(R.string.settings_privacy)) { actions.onOpenUrl(PRIVACY_URL) }
        LinkRow(stringResource(R.string.settings_license)) { actions.onOpenUrl(LICENSE_URL) }
        LinkRow(
            title = stringResource(R.string.settings_permissions_title),
            subtitle = stringResource(R.string.settings_permissions_subtitle),
            onClick = actions.onOpenPermissions,
        )
    }
}

@Composable
private fun BatteryCard(onRequest: () -> Unit) {
    val spacing = LocalSpacing.current
    CmCard(modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small)) {
        Text(stringResource(R.string.settings_battery_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_battery_body), style = MaterialTheme.typography.bodySmall)
        CmButton(
            text = stringResource(R.string.settings_battery_action),
            onClick = onRequest,
            modifier = Modifier.padding(top = spacing.small).fillMaxWidth(),
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.large, vertical = spacing.medium),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun ThemeMode.labelRes() =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
