package com.mealora.plan

import com.mealora.plan.model.FirstDayOfWeek
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.util.WeekUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekUtilsTest {

    // 2024-07-10 is a Wednesday.
    private val wed = LocalDate.of(2024, 7, 10)

    @Test
    fun mondayBasedWeekStart() {
        val start = WeekUtils.weekStart(wed, FirstDayOfWeek.Monday)
        assertEquals(LocalDate.of(2024, 7, 8), start)
    }

    @Test
    fun sundayBasedWeekStart() {
        val start = WeekUtils.weekStart(wed, FirstDayOfWeek.Sunday)
        assertEquals(LocalDate.of(2024, 7, 7), start)
    }

    @Test
    fun sevenDayGeneration() {
        val start = LocalDate.of(2024, 7, 8)
        val dates = WeekUtils.weekDates(start)
        assertEquals(7, dates.size)
        assertEquals(start, dates.first())
        assertEquals(LocalDate.of(2024, 7, 14), dates.last())
    }

    @Test
    fun previousAndNextWeek() {
        val start = LocalDate.of(2024, 7, 8)
        assertEquals(LocalDate.of(2024, 7, 1), WeekUtils.previousWeekStart(start))
        assertEquals(LocalDate.of(2024, 7, 15), WeekUtils.nextWeekStart(start))
    }

    @Test
    fun weekEndIsSixDaysLater() {
        val start = LocalDate.of(2024, 7, 8)
        assertEquals(LocalDate.of(2024, 7, 14), WeekUtils.weekEnd(start))
    }

    @Test
    fun currentWeekDetection() {
        val today = LocalDate.of(2024, 7, 10)
        val start = WeekUtils.weekStart(today, FirstDayOfWeek.Monday)
        assertTrue(WeekUtils.isCurrentWeek(start, FirstDayOfWeek.Monday, today))
        assertFalse(WeekUtils.isCurrentWeek(WeekUtils.previousWeekStart(start), FirstDayOfWeek.Monday, today))
    }

    @Test
    fun dayIndexWithinAndOutside() {
        val start = LocalDate.of(2024, 7, 8)
        assertEquals(0, WeekUtils.dayIndex(start, start))
        assertEquals(2, WeekUtils.dayIndex(LocalDate.of(2024, 7, 10), start))
        assertEquals(-1, WeekUtils.dayIndex(LocalDate.of(2024, 7, 20), start))
    }

    @Test
    fun dateForDayIndexIsClamped() {
        val start = LocalDate.of(2024, 7, 8)
        assertEquals(start, WeekUtils.dateForDayIndex(start, -5))
        assertEquals(LocalDate.of(2024, 7, 14), WeekUtils.dateForDayIndex(start, 99))
    }

    @Test
    fun filledSlotCountAndEmptyDetection() {
        val entries = listOf(
            entry("2024-07-08", MealSlot.Breakfast),
            entry("2024-07-08", MealSlot.Lunch),
            entry("2024-07-08", MealSlot.Lunch), // duplicate slot counts once
        )
        assertEquals(2, WeekUtils.filledSlotCount(entries, "2024-07-08"))
        assertFalse(WeekUtils.isDayEmpty(entries, "2024-07-08"))
        assertTrue(WeekUtils.isDayEmpty(entries, "2024-07-09"))
    }

    @Test
    fun weekEmptyDetection() {
        val week = WeekUtils.weekDates(LocalDate.of(2024, 7, 8)).map { it.toString() }
        assertTrue(WeekUtils.isWeekEmpty(emptyList(), week))
        assertFalse(WeekUtils.isWeekEmpty(listOf(entry("2024-07-10", MealSlot.Dinner)), week))
    }

    @Test
    fun entriesBySlotGroupsCorrectly() {
        val entries = listOf(
            entry("2024-07-08", MealSlot.Breakfast),
            entry("2024-07-08", MealSlot.Dinner),
        )
        val grouped = WeekUtils.entriesBySlot(entries, "2024-07-08")
        assertEquals(1, grouped[MealSlot.Breakfast]?.size)
        assertEquals(0, grouped[MealSlot.Lunch]?.size)
        assertEquals(1, grouped[MealSlot.Dinner]?.size)
    }

    private fun entry(date: String, slot: MealSlot) = MealEntry(
        id = "id-$date-$slot",
        weeklyPlanId = "p",
        date = date,
        mealSlot = slot,
        customMealName = "Meal",
    )
}
