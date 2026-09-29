package com.matheus.callmanager.feature.rules

import androidx.lifecycle.ViewModel
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.datastore.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RulesViewModel
    @Inject
    constructor(
        repository: CallRepository,
    ) : ViewModel() {
        val settings: SettingsStore = repository.settings
    }
