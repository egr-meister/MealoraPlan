package com.mealora.plan.util

import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.model.WeeklyTemplate

/**
 * Pure, deterministic operations for repeating and copying meals, and applying
 * templates. Each function takes the full list of meal entries and returns a new
 * list — nothing is mutated in place and nothing throws. Conflict handling never
 * silently deletes existing meals unless [MergeMode.Replace] is explicitly chosen.
 */
object PlanOperations {

    private fun buildCopy(
        source: MealEntry,
        date: String,
        slot: MealSlot,
        weeklyPlanId: String,
        now: String,
        repeatedFrom: String?,
    ): MealEntry = source.copy(
        id = Ids.newId(),
        weeklyPlanId = weeklyPlanId,
        date = date,
        mealSlot = slot,
        repeatedFromEntryId = repeatedFrom,
        createdAt = now,
        updatedAt = now,
    )

    /**
     * Add [candidate] into [working] respecting [mode] for the candidate's
     * date+slot. Returns the updated list. [added] tracks slots already filled
     * during this operation so FillEmptyOnly is consistent within one batch.
     */
    private fun applyOne(
        working: MutableList<MealEntry>,
        candidate: MealEntry,
        mode: MergeMode,
    ) {
        val slotOccupied = working.any { it.date == candidate.date && it.mealSlot == candidate.mealSlot }
        when (mode) {
            MergeMode.AddAll -> working.add(candidate)
            MergeMode.FillEmptyOnly -> if (!slotOccupied) working.add(candidate)
            MergeMode.Replace -> {
                working.removeAll { it.date == candidate.date && it.mealSlot == candidate.mealSlot }
                working.add(candidate)
            }
        }
    }

    /** Repeat a single meal entry onto the given target dates using [targetSlot]. */
    fun repeatMeal(
        all: List<MealEntry>,
        source: MealEntry,
        targetDates: List<String>,
        targetSlot: MealSlot,
        weeklyPlanIdForDate: (String) -> String,
        mode: MergeMode,
        now: String,
    ): List<MealEntry> {
        val working = all.toMutableList()
        for (date in targetDates.distinct()) {
            // Do not create a self-duplicate on the exact same date+slot as the source.
            if (date == source.date && targetSlot == source.mealSlot) continue
            val copy = buildCopy(
                source = source,
                date = date,
                slot = targetSlot,
                weeklyPlanId = weeklyPlanIdForDate(date),
                now = now,
                repeatedFrom = source.id,
            )
            applyOne(working, copy, mode)
        }
        return working
    }

    /** Copy every meal from [sourceDate] to [targetDate]. */
    fun copyDay(
        all: List<MealEntry>,
        sourceDate: String,
        targetDate: String,
        targetWeeklyPlanId: String,
        mode: MergeMode,
        now: String,
    ): List<MealEntry> {
        if (sourceDate == targetDate) return all
        val working = all.toMutableList()
        val sources = all.filter { it.date == sourceDate }
        if (mode == MergeMode.Replace) {
            working.removeAll { it.date == targetDate }
        }
        for (src in sources) {
            val copy = buildCopy(src, targetDate, src.mealSlot, targetWeeklyPlanId, now, null)
            // In Replace mode the target day is already cleared; add directly.
            val effectiveMode = if (mode == MergeMode.Replace) MergeMode.AddAll else mode
            applyOne(working, copy, effectiveMode)
        }
        return working
    }

    /** Copy an entire week, mapping day-for-day into the target week. */
    fun copyWeek(
        all: List<MealEntry>,
        sourceWeekDates: List<String>,
        targetWeekDates: List<String>,
        targetWeeklyPlanId: String,
        mode: MergeMode,
        now: String,
    ): List<MealEntry> {
        val working = all.toMutableList()
        if (mode == MergeMode.Replace) {
            working.removeAll { targetWeekDates.contains(it.date) }
        }
        val sources = all.filter { sourceWeekDates.contains(it.date) }
        for (src in sources) {
            val idx = sourceWeekDates.indexOf(src.date)
            if (idx < 0 || idx >= targetWeekDates.size) continue
            val targetDate = targetWeekDates[idx]
            val copy = buildCopy(src, targetDate, src.mealSlot, targetWeeklyPlanId, now, null)
            val effectiveMode = if (mode == MergeMode.Replace) MergeMode.AddAll else mode
            applyOne(working, copy, effectiveMode)
        }
        return working
    }

    /** Apply a weekly template onto the target week. */
    fun applyTemplate(
        all: List<MealEntry>,
        template: WeeklyTemplate,
        targetWeekDates: List<String>,
        targetWeeklyPlanId: String,
        mode: MergeMode,
        now: String,
    ): List<MealEntry> {
        val working = all.toMutableList()
        if (mode == MergeMode.Replace) {
            working.removeAll { targetWeekDates.contains(it.date) }
        }
        for (t in template.entries) {
            val idx = t.dayIndex.coerceIn(0, targetWeekDates.size - 1)
            if (idx < 0 || idx >= targetWeekDates.size) continue
            val date = targetWeekDates[idx]
            val entry = MealEntry(
                id = Ids.newId(),
                weeklyPlanId = targetWeeklyPlanId,
                date = date,
                mealSlot = t.mealSlot,
                dishId = t.dishId,
                customMealName = t.customMealName,
                servingsLabel = t.servingsLabel,
                note = t.note,
                repeatedFromEntryId = null,
                createdAt = now,
                updatedAt = now,
            )
            val effectiveMode = if (mode == MergeMode.Replace) MergeMode.AddAll else mode
            applyOne(working, entry, effectiveMode)
        }
        return working
    }

    /** Build template entries from a week's meal entries (for "save week as template"). */
    fun templateEntriesFromWeek(
        weekEntries: List<MealEntry>,
        weekDates: List<String>,
    ): List<com.mealora.plan.model.TemplateMealEntry> {
        return weekEntries.mapNotNull { e ->
            val idx = weekDates.indexOf(e.date)
            if (idx < 0) return@mapNotNull null
            com.mealora.plan.model.TemplateMealEntry(
                id = Ids.newId(),
                dayIndex = idx,
                mealSlot = e.mealSlot,
                dishId = e.dishId,
                customMealName = e.customMealName,
                servingsLabel = e.servingsLabel,
                note = e.note,
            )
        }
    }
}
