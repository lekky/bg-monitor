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

        // Handle any xDrip+ broadcast action
        if (intent.action?.contains("dexdrip", ignoreCase = true) == true ||
            intent.action == XDRIP_ACTION_NEW_BG_ESTIMATE) {
            handleXDripData(context, intent)
        } else {
            Log.w(TAG, "Received unknown action: ${intent.action}")
        }
    }

    private fun sendLog(context: Context, message: String) {
        Log.d(TAG, message)
        // Make log broadcast explicit too
        context.sendBroadcast(Intent(MainActivity.ACTION_LOG_MESSAGE).apply {
            setPackage(context.packageName)
            putExtra(MainActivity.EXTRA_LOG_MESSAGE, message)
        })
    }

    private fun handleXDripData(context: Context, intent: Intent) {
        try {
            sendLog(context, "→ Processing xDrip+ data...")

            // Extract data from xDrip broadcast - try multiple field names
            // Try with xDrip+ prefix first (com.eveningoutpost.dexdrip.Extras.*)
            var glucose = intent.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 0.0)
            if (glucose == 0.0) glucose = intent.getDoubleExtra("bgEstimate", 0.0)
            if (glucose == 0.0) glucose = intent.getDoubleExtra("bg", 0.0)
            if (glucose == 0.0) glucose = intent.getDoubleExtra("glucoseLevel", 0.0)
            if (glucose == 0.0) glucose = intent.getDoubleExtra("sgv", 0.0) // Nightscout format

            var timestamp = intent.getLongExtra("com.eveningoutpost.dexdrip.Extras.Time", 0L)
            if (timestamp == 0L) timestamp = intent.getLongExtra("timestamp", 0L)
            if (timestamp == 0L) timestamp = System.currentTimeMillis()

            var slopeArrow = intent.getStringExtra("com.eveningoutpost.dexdrip.Extras.BgSlopeName") ?: ""
            if (slopeArrow.isEmpty()) slopeArrow = intent.getStringExtra("slopeName") ?: ""
            if (slopeArrow.isEmpty()) slopeArrow = intent.getStringExtra("slope_name") ?: ""
            if (slopeArrow.isEmpty()) slopeArrow = intent.getStringExtra("direction") ?: ""

            var delta = intent.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", 0.0)
            if (delta == 0.0) delta = intent.getDoubleExtra("delta", 0.0)
            if (delta == 0.0) delta = intent.getDoubleExtra("bgDelta", 0.0)

            sendLog(context, "→ Extracted values:")
            sendLog(context, "  glucose=$glucose mg/dL")
            sendLog(context, "  slope=$slopeArrow")
            sendLog(context, "  delta=$delta")

            if (glucose > 0) {
                val bgData = BGData(
                    glucose = glucose,
                    timestamp = timestamp,
                    slopeArrow = slopeArrow,
                    delta = delta
                )

                sendLog(context, "✓ Valid BG data: ${bgData.getGlucoseInt()} mg/dL ${bgData.getTrendArrow()}")

                // Broadcast to the app - make it explicit to ensure delivery
                val localIntent = Intent(ACTION_BG_UPDATE).apply {
                    setPackage(context.packageName) // Explicit broadcast to our app
                    putExtra(EXTRA_GLUCOSE, glucose)
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                    putExtra(EXTRA_SLOPE_ARROW, slopeArrow)
                    putExtra(EXTRA_DELTA, delta)
                }
                context.sendBroadcast(localIntent)
                sendLog(context, "✓ Sent BG_UPDATE broadcast (explicit to ${context.packageName})")

                // Update notification if service is running
                BGMonitorService.updateNotification(context, bgData)
                sendLog(context, "✓ Notification updated")
            } else {
                sendLog(context, "⚠ ERROR: No valid glucose value!")
            }
        } catch (e: Exception) {
            sendLog(context, "❌ ERROR: ${e.message}")
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
