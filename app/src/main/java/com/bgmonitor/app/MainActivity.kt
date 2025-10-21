package com.bgmonitor.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bgmonitor.app.databinding.ActivityMainBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBGData: BGData? = null

    private val bgUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == XDripReceiver.ACTION_BG_UPDATE) {
                val glucose = intent.getDoubleExtra(XDripReceiver.EXTRA_GLUCOSE, 0.0)
                val timestamp = intent.getLongExtra(XDripReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
                val slopeArrow = intent.getStringExtra(XDripReceiver.EXTRA_SLOPE_ARROW) ?: ""
                val delta = intent.getDoubleExtra(XDripReceiver.EXTRA_DELTA, 0.0)

                val bgData = BGData(
                    glucose = glucose,
                    timestamp = timestamp,
                    slopeArrow = slopeArrow,
                    delta = delta
                )

                updateUI(bgData)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        // Start the foreground service
        BGMonitorService.start(this)

        // Register receiver for BG updates
        val filter = IntentFilter(XDripReceiver.ACTION_BG_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bgUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bgUpdateReceiver, filter)
        }

        binding.tvStatus.text = "Service Status: Running\nWaiting for xDrip broadcasts..."

        Log.d(TAG, "MainActivity started, listening for xDrip broadcasts")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bgUpdateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    private fun updateUI(bgData: BGData) {
        currentBGData = bgData

        val glucoseValue = bgData.getGlucoseInt()
        val trendArrow = bgData.getTrendArrow()
        val deltaText = if (bgData.delta != 0.0) {
            val sign = if (bgData.delta > 0) "+" else ""
            " (${sign}${String.format("%.1f", bgData.delta)})"
        } else ""

        binding.tvBGValue.text = "$glucoseValue"
        binding.tvTrend.text = "$trendArrow$deltaText"
        binding.tvLastUpdate.text = getString(R.string.last_update, bgData.getFormattedTime())

        // Set color based on glucose value
        val color = ContextCompat.getColor(this, bgData.getColorForValue())
        binding.tvBGValue.setTextColor(color)

        // Update status with raw data for debugging
        val debugInfo = JSONObject().apply {
            put("glucose", glucoseValue)
            put("timestamp", bgData.getFormattedTime())
            put("trend", bgData.slopeArrow)
            put("delta", bgData.delta)
        }
        binding.tvStatus.text = "Service Status: Active\nLast Update: ${bgData.getFormattedTime()}"
        binding.tvRawData.text = "Raw data:\n${debugInfo.toString(2)}"

        Log.d(TAG, "UI updated with BG: $glucoseValue mg/dL")
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
