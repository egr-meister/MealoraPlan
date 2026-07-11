package com.mealora.plan.util

import com.mealora.plan.model.FirstDayOfWeek
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Deterministic weekly date calculations. Supports Monday or Sunday as the
 * first day of the week. All functions return safe values and never throw.
 */
object WeekUtils {

    const val DAYS_IN_WEEK = 7
    const val SLOTS_PER_DAY = 4

    /** The [DayOfWeek] that a week starts on for the given preference. */
    fun firstDayOfWeek(pref: FirstDayOfWeek): DayOfWeek =
        when (pref) {
            FirstDayOfWeek.Monday -> DayOfWeek.MONDAY
            FirstDayOfWeek.Sunday -> DayOfWeek.SUNDAY
        }

    /** The Monday/Sunday that begins the week containing [date]. */
    fun weekStart(date: LocalDate, pref: FirstDayOfWeek): LocalDate {
        val first = firstDayOfWeek(pref)
        // Number of days to subtract to reach the first day of week.
        val diff = ((date.dayOfWeek.value - first.value) + DAYS_IN_WEEK) % DAYS_IN_WEEK
        return date.minusDays(diff.toLong())
    }

    /** The last day (inclusive) of the week that begins at [weekStart]. */
    fun weekEnd(weekStart: LocalDate): LocalDate = weekStart.plusDays((DAYS_IN_WEEK - 1).toLong())

    /** The seven dates of the week beginning at [weekStart]. */
    fun weekDates(weekStart: LocalDate): List<LocalDate> =
        (0 until DAYS_IN_WEEK).map { weekStart.plusDays(it.toLong()) }

    fun previousWeekStart(weekStart: LocalDate): LocalDate = weekStart.minusDays(DAYS_IN_WEEK.toLong())

    fun nextWeekStart(weekStart: LocalDate): LocalDate = weekStart.plusDays(DAYS_IN_WEEK.toLong())

    /** True if [weekStart] is the start of the week containing today. */
    fun isCurrentWeek(weekStart: LocalDate, pref: FirstDayOfWeek, today: LocalDate = DateUtils.today()): Boolean =
        weekStart(today, pref) == weekStart

    /** Day index 0..6 for [date] within the week beginning [weekStart]; -1 if outside. */
    fun dayIndex(date: LocalDate, weekStart: LocalDate): Int {
        val end = weekEnd(weekStart)
        if (date.isBefore(weekStart) || date.isAfter(end)) return -1
        return (date.toEpochDay() - weekStart.toEpochDay()).toInt()
    }

    /** Map a template day index (0..6) to an actual date in the target week. */
    fun dateForDayIndex(weekStart: LocalDate, dayIndex: Int): LocalDate =
        weekStart.plusDays(dayIndex.coerceIn(0, DAYS_IN_WEEK - 1).toLong())

    /** A human readable range, e.g. "Jul 6 – Jul 12". */
    fun formatRange(weekStart: LocalDate): String {
        val end = weekEnd(weekStart)
        return "${DateUtils.formatMedium(weekStart)} – ${DateUtils.formatMedium(end)}"
    }

    // ---- Meal-entry grouping helpers (pure) ----

    /** Group meal entries by their date string. */
    fun groupByDate(entries: List<MealEntry>): Map<String, List<MealEntry>> =
        entries.groupBy { it.date }

    /** Meal entries for a specific date, grouped by slot in slot order. */
    fun entriesBySlot(entries: List<MealEntry>, date: String): Map<MealSlot, List<MealEntry>> {
        val forDate = entries.filter { it.date == date }
        return MealSlot.ordered.associateWith { slot -> forDate.filter { it.mealSlot == slot } }
    }

    /** How many of the four slots have at least one entry on [date]. */
    fun filledSlotCount(entries: List<MealEntry>, date: String): Int {
        val slots = entries.filter { it.date == date }.map { it.mealSlot }.toSet()
        return slots.size
    }

    /** True when a day has no meal entries. */
    fun isDayEmpty(entries: List<MealEntry>, date: String): Boolean =
        entries.none { it.date == date }

    /** True when a week (given its seven date strings) has no meal entries. */
    fun isWeekEmpty(entries: List<MealEntry>, weekDates: List<String>): Boolean =
        entries.none { weekDates.contains(it.date) }
}
