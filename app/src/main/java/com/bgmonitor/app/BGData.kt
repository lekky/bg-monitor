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
            glucose < 70 -> R.color.bg_low
            glucose < 180 -> R.color.bg_normal
            glucose < 250 -> R.color.bg_high
            else -> R.color.bg_very_high
        }
    }
}
