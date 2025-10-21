package com.bgmonitor.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bgmonitor.app.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBGData: BGData? = null
    private val logMessages = mutableListOf<String>()
    private var useMmol = true // Default to mmol/L

    // Receiver for xDrip+ broadcasts (direct)
    private val xDripDirectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return

            addLog("★ XDRIP BROADCAST RECEIVED! ★")
            addLog("Action: ${intent.action}")

            // Log all extras
            intent.extras?.let { bundle ->
                for (key in bundle.keySet()) {
                    addLog("  $key = ${bundle.get(key)}")
                }
            }

            // Extract and log glucose data
            val glucose = intent.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 0.0)
            val slope = intent.getStringExtra("com.eveningoutpost.dexdrip.Extras.BgSlopeName") ?: ""
            addLog("→ Extracting: glucose=$glucose, slope=$slope")

            // Forward to XDripReceiver for processing
            XDripReceiver().onReceive(context, intent)
        }
    }

    private val bgUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == XDripReceiver.ACTION_BG_UPDATE) {
                val glucose = intent.getDoubleExtra(XDripReceiver.EXTRA_GLUCOSE, 0.0)
                val timestamp = intent.getLongExtra(XDripReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
                val slopeArrow = intent.getStringExtra(XDripReceiver.EXTRA_SLOPE_ARROW) ?: ""
                val delta = intent.getDoubleExtra(XDripReceiver.EXTRA_DELTA, 0.0)

                addLog("✓ BG UPDATE RECEIVED from XDripReceiver")
                addLog("  glucose=$glucose, slope=$slopeArrow, delta=$delta")

                val bgData = BGData(
                    glucose = glucose,
                    timestamp = timestamp,
                    slopeArrow = slopeArrow,
                    delta = delta
                )

                updateUI(bgData)
                addLog("✓ UI updated with BG: ${bgData.getGlucoseInt()} mg/dL")
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

    private fun exportLogs() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "bgmonitor_logs_$timestamp.txt"

            // Create file in cache directory
            val file = File(cacheDir, filename)

            // Write logs to file
            FileWriter(file).use { writer ->
                writer.write("BG Monitor Debug Logs\n")
                writer.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("=".repeat(50) + "\n\n")

                // Write logs in reverse order (newest first)
                logMessages.asReversed().forEach { log ->
                    writer.write("$log\n")
                }

                writer.write("\n" + "=".repeat(50) + "\n")
                writer.write("End of logs\n")
            }

            // Share the file using FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BG Monitor Logs")
                putExtra(Intent.EXTRA_TEXT, "Debug logs from BG Monitor app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export Logs"))
            addLog("Logs exported to $filename")
            Toast.makeText(this, "Logs exported successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs", e)
            addLog("ERROR exporting logs: ${e.message}")
            Toast.makeText(this, "Error exporting logs: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

        // Register receiver for xDrip+ broadcasts DIRECTLY
        val xDripFilter = IntentFilter().apply {
            addAction("com.eveningoutpost.dexdrip.BgEstimate")
            // Some xDrip+ variants use different actions
            addAction("com.eveningoutpost.dexdrip.g5.BgEstimate")
            addAction("com.eveningoutpost.dexdrip.NS_EMULATOR")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(xDripDirectReceiver, xDripFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(xDripDirectReceiver, xDripFilter)
        }
        addLog("✓ Registered for xDrip+ broadcasts (EXPORTED)")

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

        // Get version info
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.tvStatus.text = "BG Monitor v$versionName\nService Status: Running\nWaiting for xDrip broadcasts..."

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

        // Export logs button
        binding.btnExportLogs.setOnClickListener {
            exportLogs()
        }

        addLog("App started")
        addLog("Listening for xDrip+ broadcasts:")
        addLog("  • com.eveningoutpost.dexdrip.BgEstimate")
        addLog("  • com.eveningoutpost.dexdrip.g5.BgEstimate")
        addLog("  • com.eveningoutpost.dexdrip.NS_EMULATOR")
        addLog("")
        addLog("Waiting for data...")

        Log.d(TAG, "MainActivity started, listening for xDrip broadcasts")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(xDripDirectReceiver)
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

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.tvStatus.text = "BG Monitor v$versionName\nService Status: Active\nLast Update: ${bgData.getFormattedTime()}"

        addLog("→ Display updated: $glucoseValue $units $trendArrow$deltaText")
        Log.d(TAG, "UI updated with BG: $glucoseValue $units")
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
