package com.bgmonitor.app

import java.text.SimpleDateFormat
import java.util.*

data class BGData(
    val glucose: Double,
    val timestamp: Long,
    val trend: String = "",
    val delta: Double = 0.0,
    val slopeArrow: String = ""
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getGlucoseInt(): Int = glucose.toInt()

    fun getGlucoseMmol(): Double = glucose / 18.018

    fun getGlucoseFormatted(useMmol: Boolean): String {
        return if (useMmol) {
            String.format("%.1f", getGlucoseMmol())
        } else {
            getGlucoseInt().toString()
        }
    }

    fun getDeltaFormatted(useMmol: Boolean): String {
        val deltaValue = if (useMmol) delta / 18.018 else delta
        val sign = if (delta > 0) "+" else ""
        return if (useMmol) {
            "$sign${String.format("%.1f", deltaValue)}"
        } else {
            "$sign${String.format("%.1f", deltaValue)}"
        }
    }

    fun getTrendArrow(): String {
        return when (slopeArrow) {
            "DoubleUp" -> "⇈"
            "SingleUp" -> "↑"
            "FortyFiveUp" -> "↗"
            "Flat" -> "→"
            "FortyFiveDown" -> "↘"
            "SingleDown" -> "↓"
            "DoubleDown" -> "⇊"
            else -> slopeArrow
        }
    }

    fun getColorForValue(): Int {
        return when {
            glucose < 70 -> R.color.bg_low       // < 3.9 mmol/L
            glucose < 180 -> R.color.bg_normal   // 3.9-10.0 mmol/L
            glucose < 250 -> R.color.bg_high     // 10.0-13.9 mmol/L
            else -> R.color.bg_very_high         // > 13.9 mmol/L
        }
    }
}
