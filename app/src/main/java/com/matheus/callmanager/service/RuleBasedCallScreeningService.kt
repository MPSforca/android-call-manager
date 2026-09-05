package com.matheus.callmanager.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import com.matheus.callmanager.data.CallRepository
import com.matheus.callmanager.data.ScreeningDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Called by Telecom for every incoming call once this app is set as the default
 * call-screening app. Must respond quickly; rule evaluation hits Room so it runs off the main thread.
 */
class RuleBasedCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: CallRepository

    override fun onCreate() {
        super.onCreate()
        repository = CallRepository.get(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
            when (val decision = repository.evaluate(number)) {
                is ScreeningDecision.Reject -> {
                    respondToCall(
                        callDetails,
                        CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipNotification(true)
                            .setSkipCallLog(false)
                            .build()
                    )
                    repository.logBlockedCall(number, decision.reason)
                }
                is ScreeningDecision.Allow -> {
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            }
        }
    }
}
