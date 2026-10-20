package com.sforca.callmanager.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.data.rule.ScreeningTrace
import com.sforca.callmanager.core.model.ScreeningDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val HISTORY_LIMIT = 20

/** One past simulation, kept so a number can be re-tested after changing a rule. */
data class SimulationEntry(
    val number: String,
    val callerName: String?,
    val trace: ScreeningTrace,
    val timestampMillis: Long,
)

data class DebugUiState(
    val trace: ScreeningTrace? = null,
    val history: List<SimulationEntry> = emptyList(),
)

@HiltViewModel
class DebugViewModel
    @Inject
    constructor(
        private val repository: CallRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DebugUiState())
        val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

        /**
         * Runs the same rule evaluation the real CallScreeningService would, without needing an actual
         * call. [number] must already be normalised the way Telecom hands it over (E.164 when possible).
         */
        fun simulateIncomingCall(
            number: String,
            callerName: String? = null,
        ) {
            viewModelScope.launch {
                val trace = repository.evaluateWithTrace(number, callerName)
                val decision = trace.decision
                if (decision is ScreeningDecision.Reject) {
                    repository.logBlockedCall(number, decision.reason)
                }
                val entry = SimulationEntry(number, callerName, trace, System.currentTimeMillis())
                _uiState.update { DebugUiState(trace, (listOf(entry) + it.history).take(HISTORY_LIMIT)) }
            }
        }

        fun clearHistory() {
            _uiState.update { DebugUiState() }
        }
    }
