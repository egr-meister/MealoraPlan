package com.mealora.plan.util

/**
 * Input length limits and trimming helpers. Preserves valid international
 * characters and internal line breaks; only trims leading/trailing whitespace.
 */
object Validation {
    const val MAX_DISH_NAME = 100
    const val MAX_CUSTOM_MEAL_NAME = 100
    const val MAX_INGREDIENT_NAME = 120
    const val MAX_QUANTITY_LABEL = 80
    const val MAX_SERVINGS_LABEL = 80

    const val MAX_MEAL_NOTE = 300
    const val MAX_DAY_NOTE = 500
    const val MAX_WEEK_NOTE = 1000
    const val MAX_PREPARATION_NOTE = 2000
    const val MAX_TEMPLATE_DESCRIPTION = 500
    const val MAX_TEMPLATE_NAME = 100
    const val MAX_WEEK_TITLE = 100
    const val MAX_SHOPPING_TITLE = 120
    const val MAX_SHOPPING_NOTE = 300

    /** Trim surrounding whitespace but preserve internal line breaks. */
    fun clean(input: String): String = input.trim()

    /** Enforce a maximum length, keeping the first [max] characters. */
    fun limit(input: String, max: Int): String =
        if (input.length <= max) input else input.substring(0, max)

    /** Remaining characters before hitting [max] (never negative). */
    fun remaining(input: String, max: Int): Int = (max - input.length).coerceAtLeast(0)

    /** A dish/meal name is valid when it has at least one non-blank character. */
    fun isNonBlank(input: String): Boolean = input.trim().isNotEmpty()
}
