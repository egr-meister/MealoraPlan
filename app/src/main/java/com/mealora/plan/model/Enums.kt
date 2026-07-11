package com.mealora.plan.model

import kotlinx.serialization.Serializable

/**
 * The four fixed meal slots supported by Mealora Plan.
 * Meal slots carry no nutritional meaning.
 */
@Serializable
enum class MealSlot(val displayName: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snack("Snack");

    companion object {
        /** Ordered slots used for rendering a day board. */
        val ordered: List<MealSlot> = listOf(Breakfast, Lunch, Dinner, Snack)

        /** Safe parse that never throws; falls back to [Breakfast]. */
        fun fromNameOrDefault(name: String?): MealSlot =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Breakfast
    }
}

/**
 * Organizational category for a personal dish. Purely descriptive; no health
 * or nutritional classification is implied.
 */
@Serializable
enum class DishCategory(val displayName: String) {
    Breakfast("Breakfast"),
    MainDish("Main Dish"),
    SideDish("Side Dish"),
    Soup("Soup"),
    Salad("Salad"),
    Snack("Snack"),
    Dessert("Dessert"),
    Drink("Drink"),
    Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?): DishCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Other
    }
}

/**
 * Shopping aisle grouping for ingredients and shopping items. Descriptive only.
 */
@Serializable
enum class ShoppingCategory(val displayName: String) {
    Produce("Produce"),
    Dairy("Dairy"),
    MeatAndFish("Meat and Fish"),
    Bakery("Bakery"),
    Pantry("Pantry"),
    Frozen("Frozen"),
    Drinks("Drinks"),
    Snacks("Snacks"),
    Household("Household"),
    Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?): ShoppingCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Other
    }
}

/** First day of the week preference. */
@Serializable
enum class FirstDayOfWeek {
    Monday,
    Sunday;

    companion object {
        fun fromNameOrDefault(name: String?): FirstDayOfWeek =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Monday
    }
}

/** How copy/apply operations should resolve conflicts with existing entries. */
enum class MergeMode {
    /** Only fill meal slots that are currently empty. */
    FillEmptyOnly,

    /** Add copied meals alongside any existing meals. */
    AddAll,

    /** Remove existing meals in the target and replace with the source. */
    Replace,
}

/** Shopping-list generation strategy. */
enum class ShoppingGenerationMode {
    /** Add only generated items that are not already present for the week/source. */
    AddMissing,

    /** Remove previously generated items for this week and regenerate. */
    ReplaceGenerated,

    /** Compute a preview without persisting anything. */
    PreviewOnly,
}
