package com.sforca.callmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.datastore.SettingsRepository
import com.sforca.callmanager.core.model.ThemeMode
import com.sforca.callmanager.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val preferences: StateFlow<UserPreferences> =
            settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

        fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settings.setThemeMode(value) }

        fun setDynamicColor(value: Boolean) = viewModelScope.launch { settings.setDynamicColor(value) }
    }
