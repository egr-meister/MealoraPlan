package com.mealora.plan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mealora.plan.model.AppData
import com.mealora.plan.model.AppSettings
import com.mealora.plan.model.Dish
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.model.ShoppingGenerationMode
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.model.WeeklyPlan
import com.mealora.plan.model.WeeklyTemplate
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Ids
import com.mealora.plan.util.PlanOperations
import com.mealora.plan.util.ShoppingCandidate
import com.mealora.plan.util.ShoppingGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mealora_plan")

/**
 * The single local repository for Mealora Plan. All data is stored on-device in
 * DataStore Preferences as serialized JSON strings. There is no network, no
 * account, no cloud sync.
 *
 * Every read is defensive: empty storage, missing keys, empty JSON and corrupted
 * JSON all resolve to safe defaults. Mutations are performed atomically inside a
 * single DataStore [edit] transaction.
 */
class MealoraRepository(private val appContext: Context) {

    private object Keys {
        val DISHES = stringPreferencesKey("dishes_json")
        val WEEKLY_PLANS = stringPreferencesKey("weekly_plans_json")
        val MEAL_ENTRIES = stringPreferencesKey("meal_entries_json")
        val WEEKLY_TEMPLATES = stringPreferencesKey("weekly_templates_json")
        val SHOPPING_ITEMS = stringPreferencesKey("shopping_items_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val DAY_NOTES = stringPreferencesKey("day_notes_json")
    }

    // ---- Reads ----

    val appData: Flow<AppData> = appContext.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> decode(prefs) }

    suspend fun snapshot(): AppData = appData.first()

    private fun decode(prefs: Preferences): AppData {
        val dishes: List<Dish> = SerializationConfig.decodeList(prefs[Keys.DISHES])
        val plans: List<WeeklyPlan> = SerializationConfig.decodeList(prefs[Keys.WEEKLY_PLANS])
        val entries: List<MealEntry> = SerializationConfig.decodeList(prefs[Keys.MEAL_ENTRIES])
        val templates: List<WeeklyTemplate> = SerializationConfig.decodeList(prefs[Keys.WEEKLY_TEMPLATES])
        val shopping: List<ShoppingItem> = SerializationConfig.decodeList(prefs[Keys.SHOPPING_ITEMS])
        val settings = decodeSettings(prefs[Keys.SETTINGS])
        val dayNotes = decodeDayNotes(prefs[Keys.DAY_NOTES])
        return AppData(
            dishes = dishes,
            weeklyPlans = plans,
            mealEntries = entries,
            weeklyTemplates = templates,
            shoppingItems = shopping,
            settings = settings,
            dayNotes = dayNotes,
        )
    }

    private fun decodeDayNotes(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            SerializationConfig.json.decodeFromString<Map<String, String>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun decodeSettings(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            SerializationConfig.json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    private fun write(prefs: androidx.datastore.preferences.core.MutablePreferences, data: AppData) {
        prefs[Keys.DISHES] = SerializationConfig.encodeList(data.dishes)
        prefs[Keys.WEEKLY_PLANS] = SerializationConfig.encodeList(data.weeklyPlans)
        prefs[Keys.MEAL_ENTRIES] = SerializationConfig.encodeList(data.mealEntries)
        prefs[Keys.WEEKLY_TEMPLATES] = SerializationConfig.encodeList(data.weeklyTemplates)
        prefs[Keys.SHOPPING_ITEMS] = SerializationConfig.encodeList(data.shoppingItems)
        prefs[Keys.SETTINGS] = SerializationConfig.json.encodeToString(data.settings)
        prefs[Keys.DAY_NOTES] = SerializationConfig.json.encodeToString(data.dayNotes)
    }

    /** Set or clear a per-day free-text note. Blank clears the note. */
    suspend fun setDayNote(date: String, note: String) = update { data ->
        val notes = data.dayNotes.toMutableMap()
        if (note.isBlank()) notes.remove(date) else notes[date] = note
        data.copy(dayNotes = notes)
    }

    /** Atomic read-modify-write of the whole aggregate. */
    private suspend fun update(transform: (AppData) -> AppData) {
        appContext.dataStore.edit { prefs ->
            val current = decode(prefs)
            val next = transform(current)
            write(prefs, next)
        }
    }

    private fun now() = DateUtils.nowTimestamp()

    // ---- Settings ----

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
        update { it.copy(settings = transform(it.settings)) }

    suspend fun completeOnboarding() =
        updateSettings { it.copy(onboardingCompleted = true) }

    suspend fun showOnboardingAgain() =
        updateSettings { it.copy(onboardingCompleted = false) }

    // ---- Dishes ----

    suspend fun upsertDish(dish: Dish): String {
        val id = dish.id.ifBlank { Ids.newId() }
        update { data ->
            val ts = now()
            val existing = data.dishes.firstOrNull { it.id == id }
            val toSave = if (existing == null) {
                dish.copy(id = id, createdAt = dish.createdAt.ifBlank { ts }, updatedAt = ts)
            } else {
                dish.copy(id = id, createdAt = existing.createdAt, updatedAt = ts)
            }
            val list = if (existing == null) data.dishes + toSave
            else data.dishes.map { if (it.id == id) toSave else it }
            data.copy(dishes = list)
        }
        return id
    }

    suspend fun archiveDish(dishId: String) = update { data ->
        data.copy(dishes = data.dishes.map {
            if (it.id == dishId) it.copy(archived = true, updatedAt = now()) else it
        })
    }

    suspend fun restoreDish(dishId: String) = update { data ->
        data.copy(dishes = data.dishes.map {
            if (it.id == dishId) it.copy(archived = false, updatedAt = now()) else it
        })
    }

    suspend fun setDishFavorite(dishId: String, favorite: Boolean) = update { data ->
        data.copy(dishes = data.dishes.map {
            if (it.id == dishId) it.copy(favorite = favorite, updatedAt = now()) else it
        })
    }

    /** True if the dish is not referenced by any meal entry or template. */
    fun isDishUnused(data: AppData, dishId: String): Boolean {
        val inMeals = data.mealEntries.any { it.dishId == dishId }
        val inTemplates = data.weeklyTemplates.any { t -> t.entries.any { it.dishId == dishId } }
        return !inMeals && !inTemplates
    }

    /** Deletes a dish only if it is unused. Returns true if deleted. */
    suspend fun deleteDishIfUnused(dishId: String): Boolean {
        val data = snapshot()
        if (!isDishUnused(data, dishId)) return false
        update { it.copy(dishes = it.dishes.filterNot { d -> d.id == dishId }) }
        return true
    }

    // ---- Weekly plans ----

    /**
     * Returns the existing plan for [weekStartDate] or creates a new one. Never
     * creates a duplicate plan for the same week.
     */
    suspend fun getOrCreateWeeklyPlan(weekStartDate: String): WeeklyPlan {
        var result: WeeklyPlan? = null
        update { data ->
            val existing = data.weeklyPlans.firstOrNull { it.weekStartDate == weekStartDate }
            if (existing != null) {
                result = existing
                data
            } else {
                val ts = now()
                val plan = WeeklyPlan(
                    id = Ids.newId(),
                    weekStartDate = weekStartDate,
                    title = "",
                    notes = "",
                    templateSourceId = null,
                    createdAt = ts,
                    updatedAt = ts,
                )
                result = plan
                data.copy(weeklyPlans = data.weeklyPlans + plan)
            }
        }
        return result ?: WeeklyPlan(id = Ids.newId(), weekStartDate = weekStartDate)
    }

    suspend fun renameWeek(weekStartDate: String, title: String) =
        upsertWeekMeta(weekStartDate) { it.copy(title = title) }

    suspend fun setWeekNotes(weekStartDate: String, notes: String) =
        upsertWeekMeta(weekStartDate) { it.copy(notes = notes) }

    private suspend fun upsertWeekMeta(weekStartDate: String, transform: (WeeklyPlan) -> WeeklyPlan) =
        update { data ->
            val ts = now()
            val existing = data.weeklyPlans.firstOrNull { it.weekStartDate == weekStartDate }
            if (existing == null) {
                val plan = transform(
                    WeeklyPlan(
                        id = Ids.newId(), weekStartDate = weekStartDate,
                        createdAt = ts, updatedAt = ts,
                    ),
                )
                data.copy(weeklyPlans = data.weeklyPlans + plan.copy(updatedAt = ts))
            } else {
                data.copy(weeklyPlans = data.weeklyPlans.map {
                    if (it.weekStartDate == weekStartDate) transform(it).copy(updatedAt = ts) else it
                })
            }
        }

    /** Removes all meal entries for a week but keeps the plan record. */
    suspend fun clearWeek(weekDates: List<String>) = update { data ->
        data.copy(mealEntries = data.mealEntries.filterNot { weekDates.contains(it.date) })
    }

    /** Deletes the plan and all its meal entries. */
    suspend fun deleteWeeklyPlan(weekStartDate: String, weekDates: List<String>) = update { data ->
        data.copy(
            weeklyPlans = data.weeklyPlans.filterNot { it.weekStartDate == weekStartDate },
            mealEntries = data.mealEntries.filterNot { weekDates.contains(it.date) },
        )
    }

    // ---- Meal entries ----

    suspend fun upsertMealEntry(entry: MealEntry): String {
        val id = entry.id.ifBlank { Ids.newId() }
        update { data ->
            val ts = now()
            val existing = data.mealEntries.firstOrNull { it.id == id }
            val toSave = if (existing == null) {
                entry.copy(id = id, createdAt = entry.createdAt.ifBlank { ts }, updatedAt = ts)
            } else {
                entry.copy(id = id, createdAt = existing.createdAt, updatedAt = ts)
            }
            val list = if (existing == null) data.mealEntries + toSave
            else data.mealEntries.map { if (it.id == id) toSave else it }
            data.copy(mealEntries = list)
        }
        return id
    }

    suspend fun deleteMealEntry(entryId: String) = update { data ->
        data.copy(mealEntries = data.mealEntries.filterNot { it.id == entryId })
    }

    suspend fun moveMealEntry(entryId: String, newDate: String, newSlot: MealSlot, newWeeklyPlanId: String) =
        update { data ->
            data.copy(mealEntries = data.mealEntries.map {
                if (it.id == entryId) {
                    it.copy(date = newDate, mealSlot = newSlot, weeklyPlanId = newWeeklyPlanId, updatedAt = now())
                } else it
            })
        }

    suspend fun clearDay(date: String) = update { data ->
        data.copy(mealEntries = data.mealEntries.filterNot { it.date == date })
    }

    suspend fun clearSlot(date: String, slot: MealSlot) = update { data ->
        data.copy(mealEntries = data.mealEntries.filterNot { it.date == date && it.mealSlot == slot })
    }

    suspend fun repeatMeal(
        source: MealEntry,
        targetDates: List<String>,
        targetSlot: MealSlot,
        mode: MergeMode,
        weeklyPlanId: String,
    ) = update { data ->
        val updated = PlanOperations.repeatMeal(
            all = data.mealEntries,
            source = source,
            targetDates = targetDates,
            targetSlot = targetSlot,
            weeklyPlanIdForDate = { weeklyPlanId },
            mode = mode,
            now = now(),
        )
        data.copy(mealEntries = updated)
    }

    suspend fun copyDay(sourceDate: String, targetDate: String, targetWeeklyPlanId: String, mode: MergeMode) =
        update { data ->
            val updated = PlanOperations.copyDay(
                all = data.mealEntries,
                sourceDate = sourceDate,
                targetDate = targetDate,
                targetWeeklyPlanId = targetWeeklyPlanId,
                mode = mode,
                now = now(),
            )
            data.copy(mealEntries = updated)
        }

    suspend fun copyWeek(
        sourceWeekDates: List<String>,
        targetWeekDates: List<String>,
        targetWeeklyPlanId: String,
        mode: MergeMode,
    ) = update { data ->
        val updated = PlanOperations.copyWeek(
            all = data.mealEntries,
            sourceWeekDates = sourceWeekDates,
            targetWeekDates = targetWeekDates,
            targetWeeklyPlanId = targetWeeklyPlanId,
            mode = mode,
            now = now(),
        )
        data.copy(mealEntries = updated)
    }

    // ---- Templates ----

    suspend fun upsertTemplate(template: WeeklyTemplate): String {
        val id = template.id.ifBlank { Ids.newId() }
        update { data ->
            val ts = now()
            val existing = data.weeklyTemplates.firstOrNull { it.id == id }
            val toSave = if (existing == null) {
                template.copy(id = id, createdAt = template.createdAt.ifBlank { ts }, updatedAt = ts)
            } else {
                template.copy(id = id, createdAt = existing.createdAt, updatedAt = ts)
            }
            val list = if (existing == null) data.weeklyTemplates + toSave
            else data.weeklyTemplates.map { if (it.id == id) toSave else it }
            data.copy(weeklyTemplates = list)
        }
        return id
    }

    suspend fun createTemplateFromWeek(name: String, description: String, weekDates: List<String>): String {
        val data = snapshot()
        val weekEntries = data.mealEntries.filter { weekDates.contains(it.date) }
        val entries = PlanOperations.templateEntriesFromWeek(weekEntries, weekDates)
        val ts = now()
        val template = WeeklyTemplate(
            id = Ids.newId(),
            name = name,
            description = description,
            entries = entries,
            createdAt = ts,
            updatedAt = ts,
        )
        return upsertTemplate(template)
    }

    suspend fun duplicateTemplate(templateId: String): String? {
        val data = snapshot()
        val source = data.weeklyTemplates.firstOrNull { it.id == templateId } ?: return null
        val ts = now()
        val copy = source.copy(
            id = Ids.newId(),
            name = source.name + " (Copy)",
            entries = source.entries.map { it.copy(id = Ids.newId()) },
            createdAt = ts,
            updatedAt = ts,
        )
        return upsertTemplate(copy)
    }

    suspend fun deleteTemplate(templateId: String) = update { data ->
        data.copy(weeklyTemplates = data.weeklyTemplates.filterNot { it.id == templateId })
    }

    suspend fun applyTemplate(templateId: String, targetWeekDates: List<String>, targetWeeklyPlanId: String, mode: MergeMode) =
        update { data ->
            val template = data.weeklyTemplates.firstOrNull { it.id == templateId } ?: return@update data
            val updated = PlanOperations.applyTemplate(
                all = data.mealEntries,
                template = template,
                targetWeekDates = targetWeekDates,
                targetWeeklyPlanId = targetWeeklyPlanId,
                mode = mode,
                now = now(),
            )
            data.copy(mealEntries = updated)
        }

    // ---- Shopping list ----

    suspend fun upsertShoppingItem(item: ShoppingItem): String {
        val id = item.id.ifBlank { Ids.newId() }
        update { data ->
            val ts = now()
            val existing = data.shoppingItems.firstOrNull { it.id == id }
            val toSave = if (existing == null) {
                item.copy(id = id, createdAt = item.createdAt.ifBlank { ts }, updatedAt = ts)
            } else {
                item.copy(id = id, createdAt = existing.createdAt, updatedAt = ts)
            }
            val list = if (existing == null) data.shoppingItems + toSave
            else data.shoppingItems.map { if (it.id == id) toSave else it }
            data.copy(shoppingItems = list)
        }
        return id
    }

    suspend fun addCustomShoppingItem(title: String, quantityLabel: String, category: ShoppingCategory, weekStartDate: String, note: String): String {
        val ts = now()
        return upsertShoppingItem(
            ShoppingItem(
                id = Ids.newId(),
                title = title,
                quantityLabel = quantityLabel,
                category = category,
                sourceDishId = null,
                sourceMealEntryId = null,
                weekStartDate = weekStartDate,
                checked = false,
                note = note,
                createdAt = ts,
                updatedAt = ts,
            ),
        )
    }

    suspend fun setShoppingChecked(itemId: String, checked: Boolean) = update { data ->
        data.copy(shoppingItems = data.shoppingItems.map {
            if (it.id == itemId) it.copy(checked = checked, updatedAt = now()) else it
        })
    }

    suspend fun deleteShoppingItem(itemId: String) = update { data ->
        data.copy(shoppingItems = data.shoppingItems.filterNot { it.id == itemId })
    }

    suspend fun clearCheckedShoppingItems() = update { data ->
        data.copy(shoppingItems = data.shoppingItems.filterNot { it.checked })
    }

    suspend fun clearWeekGeneratedShoppingItems(weekStartDate: String) = update { data ->
        data.copy(shoppingItems = data.shoppingItems.filterNot {
            it.weekStartDate == weekStartDate && it.sourceDishId != null
        })
    }

    /**
     * Preview grouped candidates for the given meal entries without persisting.
     */
    fun previewShopping(entries: List<MealEntry>, dishesById: Map<String, Dish>, includedIngredientIds: Set<String>? = null): List<ShoppingCandidate> =
        ShoppingGenerator.buildCandidates(entries, dishesById, includedIngredientIds)

    /** Generate and merge shopping items from the given candidates. */
    suspend fun generateShopping(
        candidates: List<ShoppingCandidate>,
        weekStartDate: String,
        mode: ShoppingGenerationMode,
    ) = update { data ->
        val generated = ShoppingGenerator.toShoppingItems(candidates, weekStartDate, now())
        val merged = ShoppingGenerator.merge(data.shoppingItems, generated, weekStartDate, mode)
        data.copy(shoppingItems = merged)
    }

    // ---- Destructive maintenance ----

    suspend fun deleteAllHistory(currentWeekDates: List<String>) = update { data ->
        // Keep the currently-open week; remove other weekly plans and their meals.
        val keptPlans = data.weeklyPlans.filter { plan ->
            currentWeekDates.contains(plan.weekStartDate) ||
                data.mealEntries.any { it.weeklyPlanId == plan.id && currentWeekDates.contains(it.date) }
        }
        data.copy(
            weeklyPlans = keptPlans,
            mealEntries = data.mealEntries.filter { currentWeekDates.contains(it.date) },
        )
    }

    suspend fun deleteArchivedDishes() = update { data ->
        val archivedIds = data.dishes.filter { it.archived }.map { it.id }.toSet()
        // Only remove archived dishes that are not referenced anywhere.
        val removable = archivedIds.filter { id ->
            data.mealEntries.none { it.dishId == id } &&
                data.weeklyTemplates.none { t -> t.entries.any { it.dishId == id } }
        }.toSet()
        data.copy(dishes = data.dishes.filterNot { removable.contains(it.id) })
    }

    /** Wipes every stored value back to defaults. */
    suspend fun resetAll() {
        appContext.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
