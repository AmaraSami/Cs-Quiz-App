package com.example.ThinkBinary

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.Toast

class InternetConnectivityReceiver : BroadcastReceiver() {
    private var dialog: AlertDialog? = null

    override fun onReceive(context: Context, intent: Intent) {
        val activity = context as? Activity ?: return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val active: NetworkInfo? = cm.activeNetworkInfo
        val isConnected = active?.isConnected == true

        if (!isConnected) {
            if (dialog?.isShowing != true) {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("No Internet Connection")
                    .setMessage("Please connect to the internet to continue.")
                    .setCancelable(false)
                    .setPositiveButton("Retry", null)

                dialog = builder.create().apply {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val nowActive = cm.activeNetworkInfo
                            val nowConnected = nowActive?.isConnected == true

                            if (nowConnected) {
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Still no connection",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    show()
                }
            }
        } else {
            dialog?.dismiss()
            dialog = null
            Toast.makeText(activity, "Back online", Toast.LENGTH_SHORT).show()
        }
    }
}
