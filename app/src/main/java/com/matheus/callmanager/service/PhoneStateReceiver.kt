package com.matheus.callmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.matheus.callmanager.data.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Watches for RINGING -> OFFHOOK transitions, i.e. the user answering a call, so that number can be
 * remembered as "accepted" for the recently-accepted-sources rule.
 */
class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastRingingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val number = lastRingingNumber
                if (number != null) {
                    val repository = CallRepository.get(context)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        repository.recordAccepted(number)
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                lastRingingNumber = null
            }
        }
    }

    companion object {
        @Volatile private var lastRingingNumber: String? = null
    }
}
