package com.mealora.plan.util

import com.mealora.plan.model.Fallbacks
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Safe date helpers. None of these throw into the Compose UI; every parse
 * returns a nullable or a safe fallback string. All calculations use
 * [LocalDate] only (no timezone-dependent instants, no network time).
 */
object DateUtils {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Parse a stored YYYY-MM-DD string; returns null on any problem. */
    fun parseOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.trim(), ISO)
        } catch (_: Exception) {
            null
        }
    }

    /** Format a LocalDate to the canonical storage string. */
    fun toStorage(date: LocalDate): String = date.format(ISO)

    /** Human readable long date, e.g. "Monday, July 6". Safe fallback if invalid. */
    fun formatFull(value: String?): String {
        val date = parseOrNull(value) ?: return Fallbacks.DATE_UNAVAILABLE
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$weekday, $month ${date.dayOfMonth}"
    }

    /** Medium date, e.g. "Jul 6". */
    fun formatMedium(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${date.dayOfMonth}"
    }

    fun formatMedium(value: String?): String {
        val date = parseOrNull(value) ?: return Fallbacks.DATE_UNAVAILABLE
        return formatMedium(date)
    }

    /** Full weekday name, e.g. "Monday". */
    fun weekdayFull(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

    /** Short weekday name, e.g. "Mon". */
    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun weekdayShort(value: String?): String {
        val date = parseOrNull(value) ?: return "—"
        return weekdayShort(date)
    }

    /** Current local device date. */
    fun today(): LocalDate = LocalDate.now()

    /** ISO-8601-ish timestamp for created/updated fields (local, no zone offset). */
    fun nowTimestamp(): String = java.time.LocalDateTime.now().toString()
}
