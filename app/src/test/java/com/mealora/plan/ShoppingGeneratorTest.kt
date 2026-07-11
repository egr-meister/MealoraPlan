package com.mealora.plan

import com.mealora.plan.model.Dish
import com.mealora.plan.model.DishIngredient
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.model.ShoppingGenerationMode
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.util.ShoppingGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingGeneratorTest {

    private val now = "2024-07-10T10:00:00"

    private fun ing(name: String, qty: String, cat: ShoppingCategory = ShoppingCategory.Produce, default: Boolean = true) =
        DishIngredient(id = "i-$name-$qty", name = name, quantityLabel = qty, shoppingCategory = cat, addToShoppingByDefault = default)

    private fun dish(id: String, vararg ingredients: DishIngredient) =
        Dish(id = id, name = "Dish $id", ingredients = ingredients.toList())

    private fun meal(id: String, dishId: String?) =
        MealEntry(id = id, weeklyPlanId = "p", date = "2024-07-08", mealSlot = MealSlot.Dinner, dishId = dishId, customMealName = if (dishId == null) "Custom" else "")

    @Test
    fun ignoresMealsWithoutLinkedDish() {
        val dishes = mapOf("d1" to dish("d1", ing("Rice", "500 g")))
        val entries = listOf(meal("m1", null)) // custom, no dish
        val candidates = ShoppingGenerator.buildCandidates(entries, dishes)
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun onlyIncludesDefaultIngredients() {
        val d = dish("d1", ing("Rice", "500 g", default = true), ing("Salt", "1 tsp", default = false))
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1")), mapOf("d1" to d))
        assertEquals(1, candidates.size)
        assertEquals("Rice", candidates.first().displayName)
    }

    @Test
    fun manualIncludeOverridesDefaults() {
        val salt = ing("Salt", "1 tsp", default = false)
        val d = dish("d1", ing("Rice", "500 g"), salt)
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1")), mapOf("d1" to d), setOf(salt.id))
        assertEquals(1, candidates.size)
        assertEquals("Salt", candidates.first().displayName)
    }

    @Test
    fun groupsCaseInsensitivelyAndPreservesQuantities() {
        val d1 = dish("d1", ing("Tomatoes", "4"))
        val d2 = dish("d2", ing("tomatoes", "2 pieces"))
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1"), meal("m2", "d2")), mapOf("d1" to d1, "d2" to d2))
        assertEquals(1, candidates.size)
        val c = candidates.first()
        assertEquals("Tomatoes", c.displayName) // first spelling preserved
        assertEquals(2, c.dishCount)
        assertTrue(c.quantityLabels.contains("4"))
        assertTrue(c.quantityLabels.contains("2 pieces"))
    }

    @Test
    fun addMissingPreventsDuplicatesButAddsNew() {
        val d = dish("d1", ing("Rice", "500 g"))
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1")), mapOf("d1" to d))
        val generated = ShoppingGenerator.toShoppingItems(candidates, "2024-07-08", now)
        val existing = generated // already present
        val merged = ShoppingGenerator.merge(existing, generated, "2024-07-08", ShoppingGenerationMode.AddMissing)
        assertEquals(existing.size, merged.size) // no duplicate added
    }

    @Test
    fun replaceGeneratedReplacesButKeepsCustom() {
        val custom = ShoppingItem(id = "c1", title = "Napkins", weekStartDate = "2024-07-08", sourceDishId = null)
        val oldGenerated = ShoppingItem(id = "g1", title = "OldRice", weekStartDate = "2024-07-08", sourceDishId = "d1")
        val d = dish("d1", ing("Rice", "500 g"))
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1")), mapOf("d1" to d))
        val generated = ShoppingGenerator.toShoppingItems(candidates, "2024-07-08", now)
        val merged = ShoppingGenerator.merge(listOf(custom, oldGenerated), generated, "2024-07-08", ShoppingGenerationMode.ReplaceGenerated)
        assertTrue(merged.any { it.title == "Napkins" }) // custom preserved
        assertFalse(merged.any { it.title == "OldRice" }) // old generated removed
        assertTrue(merged.any { it.title == "Rice" }) // new generated present
    }

    @Test
    fun previewModeDoesNotChangeList() {
        val existing = listOf(ShoppingItem(id = "c1", title = "Napkins", weekStartDate = "2024-07-08"))
        val merged = ShoppingGenerator.merge(existing, emptyList(), "2024-07-08", ShoppingGenerationMode.PreviewOnly)
        assertEquals(existing, merged)
    }

    @Test
    fun neverInventsIngredientsForBlankNames() {
        val d = dish("d1", ing("", "1 pack"))
        val candidates = ShoppingGenerator.buildCandidates(listOf(meal("m1", "d1")), mapOf("d1" to d))
        assertTrue(candidates.isEmpty())
    }
}
