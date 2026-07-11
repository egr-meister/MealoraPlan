package com.mealora.plan.util

import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.PromptSettings
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.model.WeeklyTemplate

/** In-app planning prompt kinds. These are shown inside the app only. */
enum class PromptType { EmptyToday, EmptyTomorrow, NoShoppingList, EmptyNextWeek, TemplateAvailable }

data class PlanningPrompt(
    val type: PromptType,
    val message: String,
    val primaryLabel: String,
)

/**
 * Evaluates local, in-app prompts. Never uses push notifications, background
 * work, alarms or the network. Prompts are neutral planning nudges — never
 * framed as health reminders.
 */
object Prompts {

    /**
     * @param currentWeekDates seven date strings for the week being viewed.
     * @param nextWeekDates seven date strings for the following week.
     */
    fun evaluate(
        settings: PromptSettings,
        entries: List<MealEntry>,
        shoppingItems: List<ShoppingItem>,
        templates: List<WeeklyTemplate>,
        todayDate: String,
        tomorrowDate: String,
        currentWeekDates: List<String>,
        currentWeekStart: String,
        nextWeekDates: List<String>,
        viewingCurrentWeek: Boolean,
    ): PlanningPrompt? {
        if (!settings.enabled) return null
        if (!viewingCurrentWeek) return null

        // Priority order: today, tomorrow, shopping, next week, template.
        if (settings.showEmptyToday && WeekUtils.filledSlotCount(entries, todayDate) < WeekUtils.SLOTS_PER_DAY &&
            currentWeekDates.contains(todayDate)
        ) {
            return PlanningPrompt(
                type = PromptType.EmptyToday,
                message = "Today has empty meal slots.",
                primaryLabel = "Plan Today",
            )
        }

        if (settings.showEmptyTomorrow && WeekUtils.isDayEmpty(entries, tomorrowDate)) {
            return PlanningPrompt(
                type = PromptType.EmptyTomorrow,
                message = "Tomorrow has no meals planned.",
                primaryLabel = "Plan Tomorrow",
            )
        }

        if (settings.showShoppingPrompt) {
            val hasWeekShopping = shoppingItems.any { it.weekStartDate == currentWeekStart }
            val weekHasMeals = !WeekUtils.isWeekEmpty(entries, currentWeekDates)
            if (weekHasMeals && !hasWeekShopping) {
                return PlanningPrompt(
                    type = PromptType.NoShoppingList,
                    message = "This week has no shopping list yet.",
                    primaryLabel = "Build List",
                )
            }
        }

        if (settings.showEmptyNextWeek && WeekUtils.isWeekEmpty(entries, nextWeekDates)) {
            return PlanningPrompt(
                type = PromptType.EmptyNextWeek,
                message = "Next week is empty.",
                primaryLabel = "Plan Next Week",
            )
        }

        if (templates.isNotEmpty() && WeekUtils.isWeekEmpty(entries, currentWeekDates)) {
            return PlanningPrompt(
                type = PromptType.TemplateAvailable,
                message = "A weekly template is available to reuse.",
                primaryLabel = "Use Template",
            )
        }

        return null
    }
}
