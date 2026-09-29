package com.matheus.callmanager.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matheus.callmanager.core.data.CallRepository
import com.matheus.callmanager.core.model.ScreeningDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel
    @Inject
    constructor(
        private val repository: CallRepository,
    ) : ViewModel() {
        private val _simulationResult = MutableStateFlow<String?>(null)
        val simulationResult: StateFlow<String?> = _simulationResult.asStateFlow()

        /** Runs the same rule evaluation the real CallScreeningService would, without needing an actual call. */
        fun simulateIncomingCall(
            number: String,
            callerName: String? = null,
        ) {
            viewModelScope.launch {
                val decision = repository.evaluate(number, callerName)
                _simulationResult.value =
                    when (decision) {
                        is ScreeningDecision.Allow -> "ALLOWED — ${decision.reason}"
                        is ScreeningDecision.Reject -> {
                            repository.logBlockedCall(number, decision.reason)
                            "REJECTED — ${decision.reason}"
                        }
                    }
            }
        }

        fun clearSimulationResult() {
            _simulationResult.value = null
        }
    }
