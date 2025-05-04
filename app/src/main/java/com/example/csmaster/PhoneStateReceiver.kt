package com.example.csmaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast

class PhoneStateReceiver : BroadcastReceiver() {

    interface PhoneStateListener {
        fun onCallOrSmsReceived()
    }

    companion object {
        var listener: PhoneStateListener? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                // For incoming calls
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    Toast.makeText(context, "Incoming call detected! Quiz will change.", Toast.LENGTH_SHORT).show()
                    listener?.onCallOrSmsReceived()
                }
            }
            "android.provider.Telephony.SMS_RECEIVED" -> {
                // For incoming SMS
                Toast.makeText(context, "SMS received! Quiz will change.", Toast.LENGTH_SHORT).show()
                listener?.onCallOrSmsReceived()
            }
        }
    }
}