package com.sforca.callmanager.screening

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.di.HiltBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches for RINGING -> OFFHOOK transitions, i.e. the user answering a call, so that number can be
 * remembered as "accepted" for the recently-accepted-sources rule.
 */
@AndroidEntryPoint
class PhoneStateReceiver : HiltBroadcastReceiver() {
    @Inject
    lateinit var repository: CallRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastRingingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val number = lastRingingNumber
                if (number != null) {
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
