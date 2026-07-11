package com.mealora.plan.model

import kotlinx.serialization.Serializable

/**
 * All persisted models. Every field has a default value so that older stored
 * JSON that is missing newly-added fields still deserializes safely
 * (backward-compatible deserialization).
 *
 * Dates are stored as ISO local-date strings (YYYY-MM-DD). Timestamps are
 * stored as ISO-8601 strings.
 */

@Serializable
data class DishIngredient(
    val id: String = "",
    val name: String = "",
    val quantityLabel: String = "",
    val shoppingCategory: ShoppingCategory = ShoppingCategory.Other,
    val addToShoppingByDefault: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class Dish(
    val id: String = "",
    val name: String = "",
    val category: DishCategory = DishCategory.Other,
    val ingredients: List<DishIngredient> = emptyList(),
    val preparationNote: String = "",
    val defaultServingsLabel: String = "",
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class WeeklyPlan(
    val id: String = "",
    val weekStartDate: String = "",
    val title: String = "",
    val notes: String = "",
    val templateSourceId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class MealEntry(
    val id: String = "",
    val weeklyPlanId: String = "",
    val date: String = "",
    val mealSlot: MealSlot = MealSlot.Breakfast,
    val dishId: String? = null,
    val customMealName: String = "",
    val servingsLabel: String = "",
    val note: String = "",
    val repeatedFromEntryId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class TemplateMealEntry(
    val id: String = "",
    val dayIndex: Int = 0,
    val mealSlot: MealSlot = MealSlot.Breakfast,
    val dishId: String? = null,
    val customMealName: String = "",
    val servingsLabel: String = "",
    val note: String = "",
)

@Serializable
data class WeeklyTemplate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val entries: List<TemplateMealEntry> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ShoppingItem(
    val id: String = "",
    val title: String = "",
    val quantityLabel: String = "",
    val category: ShoppingCategory = ShoppingCategory.Other,
    val sourceDishId: String? = null,
    val sourceMealEntryId: String? = null,
    val weekStartDate: String = "",
    val checked: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class PromptSettings(
    val enabled: Boolean = true,
    val showEmptyToday: Boolean = true,
    val showEmptyTomorrow: Boolean = true,
    val showEmptyNextWeek: Boolean = true,
    val showShoppingPrompt: Boolean = true,
)

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.Monday,
    val defaultMealSlot: MealSlot = MealSlot.Breakfast,
    val openCurrentWeekByDefault: Boolean = true,
    val showEmptyMealSlots: Boolean = true,
    val promptSettings: PromptSettings = PromptSettings(),
)

/**
 * The single serialized aggregate persisted to DataStore. Individual lists are
 * also stored under separate keys, but this aggregate is convenient for tests
 * and reset operations.
 */
@Serializable
data class AppData(
    val dishes: List<Dish> = emptyList(),
    val weeklyPlans: List<WeeklyPlan> = emptyList(),
    val mealEntries: List<MealEntry> = emptyList(),
    val weeklyTemplates: List<WeeklyTemplate> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    /** Free-text day notes keyed by date string (YYYY-MM-DD). */
    val dayNotes: Map<String, String> = emptyMap(),
) {
    companion object {
        val EMPTY = AppData()
    }
}

object Fallbacks {
    const val DELETED_DISH = "Deleted Dish"
    const val UNTITLED_WEEK = "Untitled Week"
    const val MEAL_NOT_FOUND = "Meal not found"
    const val WEEK_NOT_FOUND = "Week not found"
    const val DATE_UNAVAILABLE = "Date unavailable"
}
