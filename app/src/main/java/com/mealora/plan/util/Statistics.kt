package com.mealora.plan.util

import com.mealora.plan.model.Dish
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.model.WeeklyTemplate

/**
 * Neutral planning statistics only. No calories, nutrition, weight, diet
 * adherence or health scoring of any kind is computed.
 */
data class PlanningStats(
    val plannedEntriesThisWeek: Int = 0,
    val filledSlotsThisWeek: Int = 0,
    val emptySlotsThisWeek: Int = 0,
    val uniqueDishesThisWeek: Int = 0,
    val repeatedDishCount: Int = 0,
    val shoppingItemsRemaining: Int = 0,
    val mealsPlannedThisMonth: Int = 0,
    val mostUsedSlot: MealSlot? = null,
    val mostPlannedDishName: String? = null,
    val savedTemplateCount: Int = 0,
    val slotDistribution: Map<MealSlot, Int> = emptyMap(),
    val filledSlotStrip: List<Int> = emptyList(),
)

object Statistics {

    fun compute(
        weekDates: List<String>,
        allEntries: List<MealEntry>,
        dishes: List<Dish>,
        shoppingItems: List<ShoppingItem>,
        templates: List<WeeklyTemplate>,
        monthPrefix: String,
    ): PlanningStats {
        val weekEntries = allEntries.filter { weekDates.contains(it.date) }
        val totalSlots = weekDates.size * MealSlot.ordered.size

        val filledPerDay = weekDates.map { date ->
            WeekUtils.filledSlotCount(weekEntries, date)
        }
        val filledSlots = filledPerDay.sum()

        val slotDistribution = MealSlot.ordered.associateWith { slot ->
            weekEntries.count { it.mealSlot == slot }
        }

        // Determine dish names for entries (linked dish or custom name).
        val dishNameById = dishes.associate { it.id to it.name }
        val entryLabels = weekEntries.map { entry ->
            entry.dishId?.let { dishNameById[it] } ?: entry.customMealName.ifBlank { "Meal" }
        }
        val labelCounts = entryLabels.groupingBy { it }.eachCount()
        val uniqueDishes = labelCounts.size
        val repeatedDishCount = labelCounts.values.count { it > 1 }
        val mostPlanned = labelCounts.maxByOrNull { it.value }?.key

        val mostUsedSlot = slotDistribution
            .filterValues { it > 0 }
            .maxByOrNull { it.value }
            ?.key

        val monthEntries = allEntries.count { it.date.startsWith(monthPrefix) }
        val shoppingRemaining = shoppingItems.count { !it.checked }

        return PlanningStats(
            plannedEntriesThisWeek = weekEntries.size,
            filledSlotsThisWeek = filledSlots,
            emptySlotsThisWeek = (totalSlots - filledSlots).coerceAtLeast(0),
            uniqueDishesThisWeek = uniqueDishes,
            repeatedDishCount = repeatedDishCount,
            shoppingItemsRemaining = shoppingRemaining,
            mealsPlannedThisMonth = monthEntries,
            mostUsedSlot = mostUsedSlot,
            mostPlannedDishName = mostPlanned,
            savedTemplateCount = templates.size,
            slotDistribution = slotDistribution,
            filledSlotStrip = filledPerDay,
        )
    }
}
