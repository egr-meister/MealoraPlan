package com.mealora.plan.util

import com.mealora.plan.model.Dish
import com.mealora.plan.model.DishIngredient
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.model.ShoppingGenerationMode
import com.mealora.plan.model.ShoppingItem

/**
 * A grouped shopping candidate produced from manually entered dish ingredients.
 * Quantity labels are preserved as separate free-text strings and never combined
 * mathematically.
 */
data class ShoppingCandidate(
    val groupKey: String,
    val displayName: String,
    val category: ShoppingCategory,
    val quantityLabels: List<String>,
    val dishCount: Int,
    val ingredientIds: List<String>,
    val firstDishId: String?,
) {
    val quantitySummary: String get() = quantityLabels.filter { it.isNotBlank() }.joinToString(", ")
    val dishesNote: String get() = if (dishCount > 1) "Needed for $dishCount dishes" else ""
}

private data class Collected(val ingredient: DishIngredient, val dishId: String)

/**
 * Stable, deterministic shopping-list generator.
 *
 * Guarantees:
 *  - Meal entries without a linked (existing) dish are ignored — no ingredients
 *    are ever invented for custom meal names.
 *  - Only ingredients with [DishIngredient.addToShoppingByDefault] are included,
 *    unless the caller passes an explicit include set.
 *  - Names are grouped case-insensitively only when their trimmed values match.
 *  - Quantity labels are preserved as separate text and never combined
 *    mathematically or converted between units.
 *  - User-created custom shopping items (no source dish) are always preserved.
 */
object ShoppingGenerator {

    private fun normalize(name: String): String = name.trim().lowercase()

    /**
     * Build grouped candidates from the given meal entries.
     *
     * @param includedIngredientIds when non-null, only these ingredient IDs are
     *   included regardless of their default flag (used by the preview screen
     *   after the user toggles individual ingredients).
     */
    fun buildCandidates(
        entries: List<MealEntry>,
        dishesById: Map<String, Dish>,
        includedIngredientIds: Set<String>? = null,
    ): List<ShoppingCandidate> {
        val collected = mutableListOf<Collected>()
        for (entry in entries) {
            val dishId = entry.dishId ?: continue
            val dish = dishesById[dishId] ?: continue // linked dish deleted -> ignore safely
            for (ing in dish.ingredients) {
                if (ing.name.isBlank()) continue
                val include = when {
                    includedIngredientIds != null -> includedIngredientIds.contains(ing.id)
                    else -> ing.addToShoppingByDefault
                }
                if (include) collected.add(Collected(ing, dishId))
            }
        }
        return group(collected)
    }

    private fun group(collected: List<Collected>): List<ShoppingCandidate> {
        val byKey = LinkedHashMap<String, MutableList<Collected>>()
        for (c in collected) {
            val key = normalize(c.ingredient.name)
            if (key.isEmpty()) continue
            byKey.getOrPut(key) { mutableListOf() }.add(c)
        }
        return byKey.map { (key, items) ->
            val display = items.first().ingredient.name.trim()
            val category = items
                .groupingBy { it.ingredient.shoppingCategory }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: ShoppingCategory.Other
            val quantities = items.map { it.ingredient.quantityLabel.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val dishIds = items.map { it.dishId }.distinct()
            ShoppingCandidate(
                groupKey = key,
                displayName = display,
                category = category,
                quantityLabels = quantities,
                dishCount = dishIds.size,
                ingredientIds = items.map { it.ingredient.id },
                firstDishId = dishIds.firstOrNull(),
            )
        }
    }

    /** Convert candidates into concrete (unsaved) shopping items for a week. */
    fun toShoppingItems(
        candidates: List<ShoppingCandidate>,
        weekStartDate: String,
        now: String,
    ): List<ShoppingItem> = candidates.map { c ->
        ShoppingItem(
            id = Ids.newId(),
            title = c.displayName,
            quantityLabel = c.quantitySummary,
            category = c.category,
            sourceDishId = c.firstDishId,
            sourceMealEntryId = null,
            weekStartDate = weekStartDate,
            checked = false,
            note = c.dishesNote,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun isGenerated(item: ShoppingItem): Boolean = item.sourceDishId != null

    /**
     * Merge freshly generated items into the existing list according to [mode].
     * Custom (user-created) items are always preserved.
     */
    fun merge(
        existing: List<ShoppingItem>,
        generated: List<ShoppingItem>,
        weekStartDate: String,
        mode: ShoppingGenerationMode,
    ): List<ShoppingItem> {
        return when (mode) {
            ShoppingGenerationMode.PreviewOnly -> existing
            ShoppingGenerationMode.ReplaceGenerated -> {
                // Drop previously generated items for THIS week; keep everything else.
                val kept = existing.filterNot { isGenerated(it) && it.weekStartDate == weekStartDate }
                kept + generated
            }
            ShoppingGenerationMode.AddMissing -> {
                val existingGeneratedKeys = existing
                    .filter { isGenerated(it) && it.weekStartDate == weekStartDate }
                    .map { normalize(it.title) }
                    .toSet()
                val toAdd = generated.filter { normalize(it.title) !in existingGeneratedKeys }
                existing + toAdd
            }
        }
    }
}
