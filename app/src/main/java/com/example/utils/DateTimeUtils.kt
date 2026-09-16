package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val localeIndo = Locale("id", "ID")

    /**
     * Returns today's date formatted as YYYY-MM-DD for database query
     */
    fun getTodayDbDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Formats YYYY-MM-DD into Indonesian display string:
     * e.g., "Rabu, 16 September 2026"
     */
    fun formatDisplayDate(dbDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dbDate) ?: Date()
            val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", localeIndo)
            formatter.format(parsed)
        } catch (_: Exception) {
            dbDate
        }
    }

    /**
     * Gets current phone time e.g., "08:15:30 WIB"
     */
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", localeIndo)
        return sdf.format(Date()) + " WIB"
    }

    /**
     * Gets formatted short day and date for headers e.g. "Rabu, 16 Sep 2026"
     */
    fun getTodayFullFormatted(): String {
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", localeIndo)
        return formatter.format(Date())
    }
}
