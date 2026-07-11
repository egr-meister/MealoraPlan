package com.mealora.plan.ui.navigation

/**
 * Central route definitions. Optional arguments use query parameters so that
 * navigation is resilient to missing IDs (a missing record always resolves to a
 * friendly fallback screen rather than a crash).
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BOARD = "board"
    const val DISHES = "dishes"
    const val SHOPPING = "shopping"
    const val TEMPLATES = "templates"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val DAY = "day"
    const val MEAL_EDIT = "meal_edit"
    const val DISH_EDIT = "dish_edit"
    const val DISH_DETAIL = "dish_detail"
    const val SHOPPING_PREVIEW = "shopping_preview"
    const val TEMPLATE_EDIT = "template_edit"
    const val HISTORY_WEEK = "history_week"

    // Argument keys
    const val ARG_DATE = "date"
    const val ARG_SLOT = "slot"
    const val ARG_ENTRY_ID = "entryId"
    const val ARG_DISH_ID = "dishId"
    const val ARG_TEMPLATE_ID = "templateId"
    const val ARG_WEEK_START = "weekStart"

    fun day(date: String) = "$DAY?$ARG_DATE=$date"

    fun mealEdit(date: String, slot: String, entryId: String? = null): String {
        val base = "$MEAL_EDIT?$ARG_DATE=$date&$ARG_SLOT=$slot"
        return if (entryId != null) "$base&$ARG_ENTRY_ID=$entryId" else base
    }

    fun dishEdit(dishId: String? = null): String =
        if (dishId != null) "$DISH_EDIT?$ARG_DISH_ID=$dishId" else DISH_EDIT

    fun dishDetail(dishId: String) = "$DISH_DETAIL?$ARG_DISH_ID=$dishId"

    fun templateEdit(templateId: String? = null): String =
        if (templateId != null) "$TEMPLATE_EDIT?$ARG_TEMPLATE_ID=$templateId" else TEMPLATE_EDIT

    fun historyWeek(weekStart: String) = "$HISTORY_WEEK?$ARG_WEEK_START=$weekStart"

    /** Top-level destinations that show the bottom navigation bar. */
    val topLevel = setOf(BOARD, DISHES, SHOPPING, TEMPLATES, HISTORY)
}
