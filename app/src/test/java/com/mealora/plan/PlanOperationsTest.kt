package com.mealora.plan

import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.model.TemplateMealEntry
import com.mealora.plan.model.WeeklyTemplate
import com.mealora.plan.util.PlanOperations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanOperationsTest {

    private val now = "2024-07-10T10:00:00"

    private fun entry(id: String, date: String, slot: MealSlot, name: String = "Meal", dish: String? = null) =
        MealEntry(id = id, weeklyPlanId = "p", date = date, mealSlot = slot, dishId = dish, customMealName = name)

    @Test
    fun repeatMealFillEmptyKeepsExisting() {
        val source = entry("s1", "2024-07-08", MealSlot.Lunch, "Soup")
        val existing = entry("e1", "2024-07-09", MealSlot.Lunch, "Salad")
        val all = listOf(source, existing)
        val result = PlanOperations.repeatMeal(
            all, source, listOf("2024-07-09", "2024-07-10"), MealSlot.Lunch, { "p" }, MergeMode.FillEmptyOnly, now,
        )
        // 2024-07-09 lunch kept (still Salad); 2024-07-10 gets a new Soup entry.
        val lunch09 = result.filter { it.date == "2024-07-09" && it.mealSlot == MealSlot.Lunch }
        assertEquals(1, lunch09.size)
        assertEquals("Salad", lunch09.first().customMealName)
        val lunch10 = result.filter { it.date == "2024-07-10" && it.mealSlot == MealSlot.Lunch }
        assertEquals(1, lunch10.size)
        assertEquals("Soup", lunch10.first().customMealName)
    }

    @Test
    fun repeatMealReplaceOverwrites() {
        val source = entry("s1", "2024-07-08", MealSlot.Lunch, "Soup")
        val existing = entry("e1", "2024-07-09", MealSlot.Lunch, "Salad")
        val result = PlanOperations.repeatMeal(
            listOf(source, existing), source, listOf("2024-07-09"), MealSlot.Lunch, { "p" }, MergeMode.Replace, now,
        )
        val lunch09 = result.filter { it.date == "2024-07-09" && it.mealSlot == MealSlot.Lunch }
        assertEquals(1, lunch09.size)
        assertEquals("Soup", lunch09.first().customMealName)
    }

    @Test
    fun repeatMealAddAllAddsSeparateEntries() {
        val source = entry("s1", "2024-07-08", MealSlot.Lunch, "Soup")
        val existing = entry("e1", "2024-07-09", MealSlot.Lunch, "Salad")
        val result = PlanOperations.repeatMeal(
            listOf(source, existing), source, listOf("2024-07-09"), MealSlot.Lunch, { "p" }, MergeMode.AddAll, now,
        )
        val lunch09 = result.filter { it.date == "2024-07-09" && it.mealSlot == MealSlot.Lunch }
        assertEquals(2, lunch09.size)
    }

    @Test
    fun repeatMealCreatesNewIdsAndLinks() {
        val source = entry("s1", "2024-07-08", MealSlot.Lunch, "Soup")
        val result = PlanOperations.repeatMeal(
            listOf(source), source, listOf("2024-07-09"), MealSlot.Lunch, { "p" }, MergeMode.AddAll, now,
        )
        val copy = result.first { it.date == "2024-07-09" }
        assertNotEquals("s1", copy.id)
        assertEquals("s1", copy.repeatedFromEntryId)
    }

    @Test
    fun copyDayAddMode() {
        val a = entry("a", "2024-07-08", MealSlot.Breakfast, "Eggs")
        val b = entry("b", "2024-07-09", MealSlot.Breakfast, "Toast")
        val result = PlanOperations.copyDay(listOf(a, b), "2024-07-08", "2024-07-09", "p", MergeMode.AddAll, now)
        assertEquals(2, result.count { it.date == "2024-07-09" && it.mealSlot == MealSlot.Breakfast })
    }

    @Test
    fun copyDayReplaceMode() {
        val a = entry("a", "2024-07-08", MealSlot.Breakfast, "Eggs")
        val b = entry("b", "2024-07-09", MealSlot.Breakfast, "Toast")
        val result = PlanOperations.copyDay(listOf(a, b), "2024-07-08", "2024-07-09", "p", MergeMode.Replace, now)
        val bf = result.filter { it.date == "2024-07-09" && it.mealSlot == MealSlot.Breakfast }
        assertEquals(1, bf.size)
        assertEquals("Eggs", bf.first().customMealName)
    }

    @Test
    fun copyWeekMergeEmptyOnly() {
        val src = listOf(
            entry("a", "2024-07-08", MealSlot.Breakfast, "Eggs"),
            entry("b", "2024-07-09", MealSlot.Lunch, "Soup"),
        )
        val target = listOf(entry("t", "2024-07-15", MealSlot.Breakfast, "Existing"))
        val sourceWeek = (8..14).map { "2024-07-%02d".format(it) }
        val targetWeek = (15..21).map { "2024-07-%02d".format(it) }
        val result = PlanOperations.copyWeek(src + target, sourceWeek, targetWeek, "p", MergeMode.FillEmptyOnly, now)
        // Target breakfast on 07-15 already existed -> kept.
        val bf = result.filter { it.date == "2024-07-15" && it.mealSlot == MealSlot.Breakfast }
        assertEquals(1, bf.size)
        assertEquals("Existing", bf.first().customMealName)
        // Lunch mapped to 07-16 is added.
        assertTrue(result.any { it.date == "2024-07-16" && it.mealSlot == MealSlot.Lunch && it.customMealName == "Soup" })
    }

    @Test
    fun copyWeekReplaceClearsTarget() {
        val src = listOf(entry("a", "2024-07-08", MealSlot.Breakfast, "Eggs"))
        val target = listOf(entry("t", "2024-07-15", MealSlot.Dinner, "Old"))
        val sourceWeek = (8..14).map { "2024-07-%02d".format(it) }
        val targetWeek = (15..21).map { "2024-07-%02d".format(it) }
        val result = PlanOperations.copyWeek(src + target, sourceWeek, targetWeek, "p", MergeMode.Replace, now)
        assertTrue(result.none { it.customMealName == "Old" })
        assertTrue(result.any { it.date == "2024-07-15" && it.customMealName == "Eggs" })
    }

    @Test
    fun templateDayMappingAndApplication() {
        val template = WeeklyTemplate(
            id = "t1", name = "T",
            entries = listOf(
                TemplateMealEntry(id = "x", dayIndex = 0, mealSlot = MealSlot.Breakfast, customMealName = "Oatmeal"),
                TemplateMealEntry(id = "y", dayIndex = 2, mealSlot = MealSlot.Dinner, customMealName = "Pasta"),
            ),
        )
        val targetWeek = (15..21).map { "2024-07-%02d".format(it) }
        val result = PlanOperations.applyTemplate(emptyList(), template, targetWeek, "p", MergeMode.FillEmptyOnly, now)
        assertTrue(result.any { it.date == "2024-07-15" && it.mealSlot == MealSlot.Breakfast && it.customMealName == "Oatmeal" })
        assertTrue(result.any { it.date == "2024-07-17" && it.mealSlot == MealSlot.Dinner && it.customMealName == "Pasta" })
    }

    @Test
    fun templateEntriesFromWeekMapsDayIndex() {
        val week = (8..14).map { "2024-07-%02d".format(it) }
        val entries = listOf(
            entry("a", "2024-07-08", MealSlot.Breakfast, "Eggs"),
            entry("b", "2024-07-10", MealSlot.Lunch, "Soup"),
        )
        val templateEntries = PlanOperations.templateEntriesFromWeek(entries, week)
        assertEquals(2, templateEntries.size)
        assertEquals(0, templateEntries.first { it.customMealName == "Eggs" }.dayIndex)
        assertEquals(2, templateEntries.first { it.customMealName == "Soup" }.dayIndex)
    }

    @Test
    fun deletedDishDoesNotBreakOperations() {
        // A meal entry referencing a non-existent dish is still copied safely.
        val src = entry("a", "2024-07-08", MealSlot.Lunch, name = "", dish = "missing-dish")
        val result = PlanOperations.copyDay(listOf(src), "2024-07-08", "2024-07-09", "p", MergeMode.AddAll, now)
        assertTrue(result.any { it.date == "2024-07-09" && it.dishId == "missing-dish" })
    }
}
