package com.mealora.plan.ui

import com.mealora.plan.model.AppData
import com.mealora.plan.model.Fallbacks
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import java.time.LocalDate

/** Per-day summary used by the ribbon and weekly overview. */
data class DaySummary(
    val date: LocalDate,
    val dateString: String,
    val weekdayShort: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val filledSlots: Int,
    val slotFilled: Map<MealSlot, Boolean>,
    val hasNote: Boolean,
)

/** The full state driving the Weekly Plate Board and Day Detail screens. */
data class WeekBoardState(
    val weekStart: LocalDate,
    val rangeLabel: String,
    val isCurrentWeek: Boolean,
    val selectedDate: LocalDate,
    val selectedDateString: String,
    val weekTitle: String,
    val weekNotes: String,
    val daySummaries: List<DaySummary>,
    val slotEntries: Map<MealSlot, List<MealEntry>>,
    val selectedDayNote: String,
    val weekIsEmpty: Boolean,
    val weekShoppingCount: Int,
) {
    companion object {
        fun empty(): WeekBoardState = WeekBoardState(
            weekStart = LocalDate.now(),
            rangeLabel = "",
            isCurrentWeek = true,
            selectedDate = LocalDate.now(),
            selectedDateString = "",
            weekTitle = "",
            weekNotes = "",
            daySummaries = emptyList(),
            slotEntries = MealSlot.ordered.associateWith { emptyList() },
            selectedDayNote = "",
            weekIsEmpty = true,
            weekShoppingCount = 0,
        )
    }
}

/** Resolve the human label for a meal entry: linked dish name or custom name. */
fun MealEntry.label(data: AppData): String {
    val linkedId = dishId
    if (linkedId != null) {
        val dish = data.dishes.firstOrNull { it.id == linkedId }
        return dish?.name?.ifBlank { Fallbacks.DELETED_DISH } ?: Fallbacks.DELETED_DISH
    }
    return customMealName.ifBlank { Fallbacks.MEAL_NOT_FOUND }
}
