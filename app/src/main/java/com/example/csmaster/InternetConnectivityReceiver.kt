package com.example.csmaster

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
        // We only handle it if the context is an Activity
        val activity = context as? Activity ?: return

        // Check current connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val active: NetworkInfo? = cm.activeNetworkInfo
        val isConnected = active?.isConnected == true

        if (!isConnected) {
            // Show dialog if not already visible
            if (dialog?.isShowing != true) {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("No Internet Connection")
                    .setMessage("Please connect to the internet to continue.")
                    .setCancelable(false)
                    // We’ll install our own click‑listener below
                    .setPositiveButton("Retry", null)

                dialog = builder.create().apply {
                    setOnShowListener {
                        // Grab the button and override its onClick
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            // Re‑check connectivity
                            val nowActive = cm.activeNetworkInfo
                            val nowConnected = nowActive?.isConnected == true

                            if (nowConnected) {
                                dismiss()  // only dismiss if we’re actually online
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Still no connection",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // remains visible
                            }
                        }
                    }
                    show()
                }
            }
        } else {
            // If dialog was up, dismiss it now we’re online
            dialog?.dismiss()
            dialog = null
            // Optional: Toast.makeText(activity, "Back online", Toast.LENGTH_SHORT).show()
        }
    }
}
