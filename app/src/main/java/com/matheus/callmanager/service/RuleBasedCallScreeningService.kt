package com.matheus.callmanager.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import com.matheus.callmanager.data.CallRepository
import com.matheus.callmanager.data.ScreeningAction
import com.matheus.callmanager.data.ScreeningDecision
import com.matheus.callmanager.data.ScreeningPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Called by Telecom for every incoming call once this app is set as the default
 * call-screening app. Must respond quickly; rule evaluation hits Room so it runs off the main thread.
 *
 * Only ever fires for real (SIM/carrier) calls: the manifest does not declare
 * android.telecom.extra.CALL_SCREENING_INCLUDE_SELF_MANAGED_CALLS, so Telecom never routes
 * self-managed connections (the mechanism VoIP apps like WhatsApp use) here in the first place.
 * The direction/self-managed check below is a defensive second layer.
 */
class RuleBasedCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: CallRepository

    override fun onCreate() {
        super.onCreate()
        repository = CallRepository.get(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val isSelfManaged = callDetails.callProperties and Call.Details.PROPERTY_SELF_MANAGED != 0
        val number = callDetails.handle?.schemeSpecificPart
        if (isSelfManaged || number == null) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
            val decision = repository.evaluate(number, callDetails.callerDisplayName)
            val action = ScreeningPolicy.determineAction(decision, repository.settings.blockedCallAction)
            respondToCall(callDetails, buildResponse(action))
            if (decision is ScreeningDecision.Reject) {
                repository.logBlockedCall(number, decision.reason)
            }
        }
    }

    private fun buildResponse(action: ScreeningAction): CallResponse = when (action) {
        ScreeningAction.ALLOW -> CallResponse.Builder().build()

        // Still shows on screen/notification, never rings or vibrates. Needs API 29+; older
        // platforms have no silence primitive, so fall back to a full reject instead.
        ScreeningAction.SILENCE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CallResponse.Builder().setSilenceCall(true).build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .build()
        }

        ScreeningAction.REJECT -> CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipNotification(true)
            .build()
    }
}
