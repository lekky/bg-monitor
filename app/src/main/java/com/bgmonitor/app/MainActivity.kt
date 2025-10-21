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
    private val logMessages = mutableListOf<String>()
    private var useMmol = true // Default to mmol/L

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
                addLog("Received BG update: ${bgData.getGlucoseInt()} mg/dL")
            }
        }
    }

    private fun testBroadcastReception() {
        addLog("Testing receiver with simulated data...")
        val testIntent = Intent(XDripReceiver.ACTION_BG_UPDATE).apply {
            putExtra(XDripReceiver.EXTRA_GLUCOSE, 120.0)
            putExtra(XDripReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
            putExtra(XDripReceiver.EXTRA_SLOPE_ARROW, "Flat")
            putExtra(XDripReceiver.EXTRA_DELTA, 0.0)
        }
        sendBroadcast(testIntent)
        addLog("Test broadcast sent!")
    }

    private fun updateUnitsButton() {
        binding.btnToggleUnits.text = if (useMmol) "mmol/L" else "mg/dL"
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"
        logMessages.add(0, logEntry)

        // Keep only last 20 messages
        if (logMessages.size > 20) {
            logMessages.removeAt(logMessages.size - 1)
        }

        runOnUiThread {
            binding.tvRawData.text = logMessages.joinToString("\n")
        }
        Log.d(TAG, message)
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

        // Load preference for units
        val prefs = getSharedPreferences("BGMonitorPrefs", MODE_PRIVATE)
        useMmol = prefs.getBoolean("useMmol", true)
        updateUnitsButton()

        binding.tvStatus.text = "Service Status: Running\nWaiting for xDrip broadcasts..."

        // Test button to simulate broadcast
        binding.btnTestBroadcast.setOnClickListener {
            testBroadcastReception()
        }

        // Toggle units button
        binding.btnToggleUnits.setOnClickListener {
            useMmol = !useMmol
            prefs.edit().putBoolean("useMmol", useMmol).apply()
            updateUnitsButton()
            // Re-render current data if available
            currentBGData?.let { updateUI(it) }
            addLog("Switched to ${if (useMmol) "mmol/L" else "mg/dL"}")
        }

        addLog("App started")
        addLog("Waiting for xDrip+ broadcasts...")
        addLog("Action: ${XDripReceiver.XDRIP_ACTION_NEW_BG_ESTIMATE}")

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

        val glucoseValue = bgData.getGlucoseFormatted(useMmol)
        val trendArrow = bgData.getTrendArrow()
        val units = if (useMmol) "mmol/L" else "mg/dL"
        val deltaText = if (bgData.delta != 0.0) {
            " (${bgData.getDeltaFormatted(useMmol)})"
        } else ""

        binding.tvBGValue.text = glucoseValue
        binding.tvTrend.text = "$trendArrow$deltaText"
        binding.tvLastUpdate.text = getString(R.string.last_update, bgData.getFormattedTime())

        // Set color based on glucose value
        val color = ContextCompat.getColor(this, bgData.getColorForValue())
        binding.tvBGValue.setTextColor(color)

        binding.tvStatus.text = "Service Status: Active\nLast Update: ${bgData.getFormattedTime()}"

        addLog("BG: $glucoseValue $units $trendArrow$deltaText")
        Log.d(TAG, "UI updated with BG: $glucoseValue $units")
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
