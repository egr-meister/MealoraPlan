package com.mealora.plan

import com.mealora.plan.model.Dish
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.util.Statistics
import com.mealora.plan.util.WeekUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatisticsTest {

    private val weekStart = LocalDate.of(2024, 7, 8)
    private val week = WeekUtils.weekDates(weekStart).map { it.toString() }

    private fun meal(date: String, slot: MealSlot, dish: String? = null, name: String = "Meal") =
        MealEntry(id = "id-$date-$slot-$name", weeklyPlanId = "p", date = date, mealSlot = slot, dishId = dish, customMealName = name)

    @Test
    fun computesNeutralPlanningStats() {
        val dishes = listOf(Dish(id = "d1", name = "Pasta"))
        val entries = listOf(
            meal("2024-07-08", MealSlot.Breakfast, name = "Oatmeal"),
            meal("2024-07-08", MealSlot.Lunch, dish = "d1", name = ""),
            meal("2024-07-09", MealSlot.Lunch, dish = "d1", name = ""),
        )
        val stats = Statistics.compute(week, entries, dishes, emptyList(), emptyList(), "2024-07")
        assertEquals(3, stats.plannedEntriesThisWeek)
        assertEquals(3, stats.filledSlotsThisWeek)
        assertEquals(25, stats.emptySlotsThisWeek) // 28 total - 3 filled
        assertEquals(2, stats.uniqueDishesThisWeek) // Oatmeal + Pasta
        assertEquals(1, stats.repeatedDishCount) // Pasta twice
        assertEquals(MealSlot.Lunch, stats.mostUsedSlot)
        assertEquals("Pasta", stats.mostPlannedDishName)
        assertEquals(3, stats.mealsPlannedThisMonth)
    }

    @Test
    fun emptyWeekProducesZeroes() {
        val stats = Statistics.compute(week, emptyList(), emptyList(), emptyList(), emptyList(), "2024-07")
        assertEquals(0, stats.plannedEntriesThisWeek)
        assertEquals(0, stats.filledSlotsThisWeek)
        assertEquals(28, stats.emptySlotsThisWeek)
    }
}
