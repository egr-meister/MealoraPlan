package com.mealora.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mealora.plan.MealoraApplication
import com.mealora.plan.data.MealoraRepository
import com.mealora.plan.model.AppData
import com.mealora.plan.model.AppSettings
import com.mealora.plan.model.Dish
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.model.ShoppingGenerationMode
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.model.WeeklyTemplate
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.PlanningStats
import com.mealora.plan.util.PlanningPrompt
import com.mealora.plan.util.Prompts
import com.mealora.plan.util.ShoppingCandidate
import com.mealora.plan.util.Statistics
import com.mealora.plan.util.WeekUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Shared planner ViewModel. Activity-scoped so every screen observes the same
 * immutable [AppData] state via [StateFlow] and the same selected day. All
 * mutations delegate to the single [MealoraRepository]; the UI never mutates
 * state directly.
 */
class PlannerViewModel(private val repo: MealoraRepository) : ViewModel() {

    private val _ready = MutableStateFlow(false)

    /** Becomes true after the first value has been read from storage. */
    val isReady: StateFlow<Boolean> = _ready

    val appData: StateFlow<AppData> = repo.appData
        .onEach { _ready.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppData.EMPTY)

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    /** Prompts dismissed during this app session (kept in memory only). */
    private val _dismissedPrompts = MutableStateFlow<Set<String>>(emptySet())

    val board: StateFlow<WeekBoardState> =
        combine(appData, _selectedDate) { data, date -> buildBoard(data, date) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekBoardState.empty())

    val prompt: StateFlow<PlanningPrompt?> =
        combine(appData, _selectedDate, _dismissedPrompts) { data, date, dismissed ->
            evaluatePrompt(data, date, dismissed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Ensure a plan record exists for the initially selected week.
        ensurePlanForSelectedWeek()
    }

    // ---- Derivations ----

    private fun weekStartFor(date: LocalDate, data: AppData): LocalDate =
        WeekUtils.weekStart(date, data.settings.firstDayOfWeek)

    private fun buildBoard(data: AppData, selected: LocalDate): WeekBoardState {
        val settings = data.settings
        val weekStart = WeekUtils.weekStart(selected, settings.firstDayOfWeek)
        val weekDates = WeekUtils.weekDates(weekStart)
        val weekDateStrings = weekDates.map { DateUtils.toStorage(it) }
        val today = DateUtils.today()
        val plan = data.weeklyPlans.firstOrNull { it.weekStartDate == DateUtils.toStorage(weekStart) }

        val daySummaries = weekDates.map { date ->
            val ds = DateUtils.toStorage(date)
            val slotFilled = MealSlot.ordered.associateWith { slot ->
                data.mealEntries.any { it.date == ds && it.mealSlot == slot }
            }
            DaySummary(
                date = date,
                dateString = ds,
                weekdayShort = DateUtils.weekdayShort(date),
                dayOfMonth = date.dayOfMonth,
                isToday = date == today,
                isSelected = date == selected,
                filledSlots = slotFilled.count { it.value },
                slotFilled = slotFilled,
                hasNote = data.dayNotes[ds]?.isNotBlank() == true,
            )
        }

        val selectedStr = DateUtils.toStorage(selected)
        val slotEntries = MealSlot.ordered.associateWith { slot ->
            data.mealEntries.filter { it.date == selectedStr && it.mealSlot == slot }
        }

        return WeekBoardState(
            weekStart = weekStart,
            rangeLabel = WeekUtils.formatRange(weekStart),
            isCurrentWeek = WeekUtils.isCurrentWeek(weekStart, settings.firstDayOfWeek, today),
            selectedDate = selected,
            selectedDateString = selectedStr,
            weekTitle = plan?.title.orEmpty(),
            weekNotes = plan?.notes.orEmpty(),
            daySummaries = daySummaries,
            slotEntries = slotEntries,
            selectedDayNote = data.dayNotes[selectedStr].orEmpty(),
            weekIsEmpty = WeekUtils.isWeekEmpty(data.mealEntries, weekDateStrings),
            weekShoppingCount = data.shoppingItems.count { it.weekStartDate == DateUtils.toStorage(weekStart) },
        )
    }

    private fun evaluatePrompt(data: AppData, selected: LocalDate, dismissed: Set<String>): PlanningPrompt? {
        val settings = data.settings.promptSettings
        val today = DateUtils.today()
        val weekStart = WeekUtils.weekStart(selected, data.settings.firstDayOfWeek)
        val weekDates = WeekUtils.weekDates(weekStart).map { DateUtils.toStorage(it) }
        val nextWeekStart = WeekUtils.nextWeekStart(weekStart)
        val nextWeekDates = WeekUtils.weekDates(nextWeekStart).map { DateUtils.toStorage(it) }
        val viewingCurrent = WeekUtils.isCurrentWeek(weekStart, data.settings.firstDayOfWeek, today)
        val prompt = Prompts.evaluate(
            settings = settings,
            entries = data.mealEntries,
            shoppingItems = data.shoppingItems,
            templates = data.weeklyTemplates,
            todayDate = DateUtils.toStorage(today),
            tomorrowDate = DateUtils.toStorage(today.plusDays(1)),
            currentWeekDates = weekDates,
            currentWeekStart = DateUtils.toStorage(weekStart),
            nextWeekDates = nextWeekDates,
            viewingCurrentWeek = viewingCurrent,
        )
        return if (prompt != null && dismissed.contains(prompt.type.name)) null else prompt
    }

    fun dismissPrompt(prompt: PlanningPrompt) {
        _dismissedPrompts.value = _dismissedPrompts.value + prompt.type.name
    }

    // ---- Week navigation ----

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        ensurePlanForSelectedWeek()
    }

    fun previousWeek() {
        _selectedDate.value = _selectedDate.value.minusDays(WeekUtils.DAYS_IN_WEEK.toLong())
        ensurePlanForSelectedWeek()
    }

    fun nextWeek() {
        _selectedDate.value = _selectedDate.value.plusDays(WeekUtils.DAYS_IN_WEEK.toLong())
        ensurePlanForSelectedWeek()
    }

    fun goToCurrentWeek() {
        _selectedDate.value = DateUtils.today()
        ensurePlanForSelectedWeek()
    }

    private fun ensurePlanForSelectedWeek() {
        viewModelScope.launch {
            val data = appData.value
            val weekStart = weekStartFor(_selectedDate.value, data)
            repo.getOrCreateWeeklyPlan(DateUtils.toStorage(weekStart))
        }
    }

    private suspend fun planIdFor(weekStart: LocalDate): String =
        repo.getOrCreateWeeklyPlan(DateUtils.toStorage(weekStart)).id

    fun selectedWeekStart(): LocalDate = weekStartFor(_selectedDate.value, appData.value)

    fun weekDateStrings(weekStart: LocalDate): List<String> =
        WeekUtils.weekDates(weekStart).map { DateUtils.toStorage(it) }

    // ---- Lookups for UI ----

    fun dishById(id: String?): Dish? = id?.let { appData.value.dishes.firstOrNull { d -> d.id == it } }

    fun mealEntryById(id: String?): MealEntry? =
        id?.let { appData.value.mealEntries.firstOrNull { e -> e.id == it } }

    fun templateById(id: String?): WeeklyTemplate? =
        id?.let { appData.value.weeklyTemplates.firstOrNull { t -> t.id == it } }

    fun shoppingItemById(id: String?): ShoppingItem? =
        id?.let { appData.value.shoppingItems.firstOrNull { s -> s.id == it } }

    // ---- Meal entry actions ----

    fun saveMealEntry(entry: MealEntry, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            val date = DateUtils.parseOrNull(entry.date) ?: _selectedDate.value
            val weekStart = weekStartFor(date, appData.value)
            val planId = planIdFor(weekStart)
            val id = repo.upsertMealEntry(entry.copy(weeklyPlanId = planId))
            onDone(id)
        }
    }

    fun deleteMealEntry(id: String) = viewModelScope.launch { repo.deleteMealEntry(id) }

    fun clearSlot(date: String, slot: MealSlot) = viewModelScope.launch { repo.clearSlot(date, slot) }

    fun clearDay(date: String) = viewModelScope.launch { repo.clearDay(date) }

    fun moveMealEntry(id: String, newDate: String, newSlot: MealSlot) {
        viewModelScope.launch {
            val date = DateUtils.parseOrNull(newDate) ?: return@launch
            val planId = planIdFor(weekStartFor(date, appData.value))
            repo.moveMealEntry(id, newDate, newSlot, planId)
        }
    }

    fun repeatMeal(source: MealEntry, targetDates: List<String>, targetSlot: MealSlot, mode: MergeMode) {
        viewModelScope.launch {
            val planId = planIdFor(weekStartFor(_selectedDate.value, appData.value))
            repo.repeatMeal(source, targetDates, targetSlot, mode, planId)
        }
    }

    fun copyDay(sourceDate: String, targetDate: String, mode: MergeMode) {
        viewModelScope.launch {
            val date = DateUtils.parseOrNull(targetDate) ?: return@launch
            val planId = planIdFor(weekStartFor(date, appData.value))
            repo.copyDay(sourceDate, targetDate, planId, mode)
        }
    }

    fun copyWeek(sourceWeekStart: LocalDate, targetWeekStart: LocalDate, mode: MergeMode) {
        viewModelScope.launch {
            val sourceDates = weekDateStrings(sourceWeekStart)
            val targetDates = weekDateStrings(targetWeekStart)
            val planId = planIdFor(targetWeekStart)
            repo.copyWeek(sourceDates, targetDates, planId, mode)
        }
    }

    fun setDayNote(date: String, note: String) = viewModelScope.launch { repo.setDayNote(date, note) }

    fun setWeekTitle(weekStart: LocalDate, title: String) =
        viewModelScope.launch { repo.renameWeek(DateUtils.toStorage(weekStart), title) }

    fun setWeekNotes(weekStart: LocalDate, notes: String) =
        viewModelScope.launch { repo.setWeekNotes(DateUtils.toStorage(weekStart), notes) }

    fun clearWeek(weekStart: LocalDate) =
        viewModelScope.launch { repo.clearWeek(weekDateStrings(weekStart)) }

    fun deleteWeek(weekStart: LocalDate) =
        viewModelScope.launch { repo.deleteWeeklyPlan(DateUtils.toStorage(weekStart), weekDateStrings(weekStart)) }

    // ---- Dish actions ----

    fun saveDish(dish: Dish, onDone: (String) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.upsertDish(dish)) }
    }

    fun archiveDish(id: String) = viewModelScope.launch { repo.archiveDish(id) }
    fun restoreDish(id: String) = viewModelScope.launch { repo.restoreDish(id) }
    fun setDishFavorite(id: String, favorite: Boolean) = viewModelScope.launch { repo.setDishFavorite(id, favorite) }

    fun deleteDishIfUnused(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteDishIfUnused(id)) }
    }

    fun isDishUnused(id: String): Boolean = repo.isDishUnused(appData.value, id)

    // ---- Template actions ----

    fun saveTemplate(template: WeeklyTemplate, onDone: (String) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.upsertTemplate(template)) }
    }

    fun createTemplateFromWeek(name: String, description: String, weekStart: LocalDate, onDone: (String) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.createTemplateFromWeek(name, description, weekDateStrings(weekStart))) }
    }

    fun duplicateTemplate(id: String) = viewModelScope.launch { repo.duplicateTemplate(id) }
    fun deleteTemplate(id: String) = viewModelScope.launch { repo.deleteTemplate(id) }

    fun applyTemplate(templateId: String, targetWeekStart: LocalDate, mode: MergeMode) {
        viewModelScope.launch {
            val planId = planIdFor(targetWeekStart)
            repo.applyTemplate(templateId, weekDateStrings(targetWeekStart), planId, mode)
        }
    }

    // ---- Shopping actions ----

    fun previewShopping(entries: List<MealEntry>, includedIngredientIds: Set<String>? = null): List<ShoppingCandidate> {
        val dishesById = appData.value.dishes.associateBy { it.id }
        return repo.previewShopping(entries, dishesById, includedIngredientIds)
    }

    fun generateShopping(candidates: List<ShoppingCandidate>, weekStart: LocalDate, mode: ShoppingGenerationMode) {
        viewModelScope.launch { repo.generateShopping(candidates, DateUtils.toStorage(weekStart), mode) }
    }

    fun addCustomShoppingItem(title: String, quantity: String, category: ShoppingCategory, weekStart: LocalDate, note: String) {
        viewModelScope.launch { repo.addCustomShoppingItem(title, quantity, category, DateUtils.toStorage(weekStart), note) }
    }

    fun updateShoppingItem(item: ShoppingItem) = viewModelScope.launch { repo.upsertShoppingItem(item) }
    fun setShoppingChecked(id: String, checked: Boolean) = viewModelScope.launch { repo.setShoppingChecked(id, checked) }
    fun deleteShoppingItem(id: String) = viewModelScope.launch { repo.deleteShoppingItem(id) }
    fun clearCheckedShopping() = viewModelScope.launch { repo.clearCheckedShoppingItems() }
    fun clearWeekGeneratedShopping(weekStart: LocalDate) =
        viewModelScope.launch { repo.clearWeekGeneratedShoppingItems(DateUtils.toStorage(weekStart)) }

    // ---- Statistics ----

    fun statsForSelectedWeek(): PlanningStats {
        val data = appData.value
        val weekStart = weekStartFor(_selectedDate.value, data)
        val monthPrefix = DateUtils.toStorage(weekStart).substring(0, 7)
        return Statistics.compute(
            weekDates = weekDateStrings(weekStart),
            allEntries = data.mealEntries,
            dishes = data.dishes,
            shoppingItems = data.shoppingItems.filter { it.weekStartDate == DateUtils.toStorage(weekStart) },
            templates = data.weeklyTemplates,
            monthPrefix = monthPrefix,
        )
    }

    // ---- Settings & maintenance ----

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { repo.updateSettings(transform) }
    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding() }
    fun showOnboardingAgain() = viewModelScope.launch { repo.showOnboardingAgain() }
    fun deleteArchivedDishes() = viewModelScope.launch { repo.deleteArchivedDishes() }

    fun deleteAllHistory() = viewModelScope.launch {
        val weekStart = weekStartFor(_selectedDate.value, appData.value)
        repo.deleteAllHistory(weekDateStrings(weekStart))
    }

    fun resetAll() = viewModelScope.launch {
        repo.resetAll()
        _selectedDate.value = DateUtils.today()
        _dismissedPrompts.value = emptySet()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MealoraApplication
                return PlannerViewModel(app.repository) as T
            }
        }
    }
}
