package com.example.ThinkBinary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        var listener: InterruptionListener? = null
    }

    interface InterruptionListener {
        fun onInterruptionDetected()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING ||
                    state == TelephonyManager.EXTRA_STATE_OFFHOOK
                ) {
                    Log.d("PhoneStateReceiver", "Call interruption detected")
                    listener?.onInterruptionDetected()
                }
            }

            "android.provider.Telephony.SMS_RECEIVED" -> {
                Log.d("PhoneStateReceiver", "SMS interruption detected")
                listener?.onInterruptionDetected()
            }
        }
    }
}
