package com.bgmonitor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

class XDripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }

        Log.d(TAG, "========== BROADCAST RECEIVED ==========")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Package: ${intent.`package`}")

        // Log all extras
        intent.extras?.let { bundle ->
            Log.d(TAG, "Extras count: ${bundle.size()}")
            for (key in bundle.keySet()) {
                Log.d(TAG, "  $key = ${bundle.get(key)}")
            }
        }

        when (intent.action) {
            XDRIP_ACTION_NEW_BG_ESTIMATE -> {
                handleXDripData(context, intent)
            }
            else -> {
                Log.w(TAG, "Received unknown action: ${intent.action}")
            }
        }
    }

    private fun handleXDripData(context: Context, intent: Intent) {
        try {
            // Extract data from xDrip broadcast
            val glucose = intent.getDoubleExtra("bgEstimate", 0.0)
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            val slopeArrow = intent.getStringExtra("slopeName") ?: ""
            val delta = intent.getDoubleExtra("delta", 0.0)

            // Log all extras for debugging
            val extras = intent.extras
            val debugInfo = JSONObject()
            extras?.keySet()?.forEach { key ->
                debugInfo.put(key, extras.get(key).toString())
            }
            Log.d(TAG, "Received data: $debugInfo")

            if (glucose > 0) {
                val bgData = BGData(
                    glucose = glucose,
                    timestamp = timestamp,
                    slopeArrow = slopeArrow,
                    delta = delta
                )

                Log.d(TAG, "Blood Glucose: ${bgData.getGlucoseInt()} mg/dL, " +
                        "Trend: ${bgData.getTrendArrow()}, Time: ${bgData.getFormattedTime()}")

                // Broadcast to the app
                val localIntent = Intent(ACTION_BG_UPDATE).apply {
                    putExtra(EXTRA_GLUCOSE, glucose)
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                    putExtra(EXTRA_SLOPE_ARROW, slopeArrow)
                    putExtra(EXTRA_DELTA, delta)
                }
                context.sendBroadcast(localIntent)

                // Update notification if service is running
                BGMonitorService.updateNotification(context, bgData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing xDrip data", e)
        }
    }

    companion object {
        private const val TAG = "XDripReceiver"
        const val XDRIP_ACTION_NEW_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate"
        const val ACTION_BG_UPDATE = "com.bgmonitor.app.BG_UPDATE"
        const val EXTRA_GLUCOSE = "glucose"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_SLOPE_ARROW = "slopeArrow"
        const val EXTRA_DELTA = "delta"
    }
}
