package com.mealora.plan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Ids
import com.mealora.plan.util.Validation
import com.mealora.plan.util.WeekUtils

@Composable
fun MealEditScreen(
    vm: PlannerViewModel,
    navController: NavHostController,
    dateArg: String,
    slotArg: String,
    entryIdArg: String,
) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val existing = remember(entryIdArg, data.mealEntries) {
        if (entryIdArg.isBlank()) null else data.mealEntries.firstOrNull { it.id == entryIdArg }
    }
    val date = DateUtils.parseOrNull(dateArg) ?: vm.selectedDate.value
    val dateStr = DateUtils.toStorage(date)

    var slot by remember { mutableStateOf(existing?.mealSlot ?: MealSlot.fromNameOrDefault(slotArg)) }
    var dishId by remember { mutableStateOf(existing?.dishId) }
    var customName by remember { mutableStateOf(existing?.customMealName.orEmpty()) }
    var servings by remember { mutableStateOf(existing?.servingsLabel.orEmpty()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    var showDishPicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }

    val selectedDish = dishId?.let { id -> data.dishes.firstOrNull { it.id == id } }
    val canSave = dishId != null || Validation.isNonBlank(customName)

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(
            title = if (existing == null) "Add meal" else "Edit meal",
            onBack = { navController.popBackStack() },
            actions = {
                if (existing != null) {
                    TextButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete meal")
                    }
                }
            },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            Text(DateUtils.formatFull(dateStr), style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))
            SectionLabel("Meal slot")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealSlot.ordered.forEach { s ->
                    FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s.displayName) })
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("Dish")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                onClick = { showDishPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        selectedDish?.name ?: "Choose an existing dish (optional)",
                        modifier = Modifier.weight(1f),
                        color = if (selectedDish != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selectedDish != null) {
                        TextButton(onClick = { dishId = null }) { Text("Clear") }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionLabel("Or custom meal name")
            LimitedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = "Custom meal name",
                maxLength = Validation.MAX_CUSTOM_MEAL_NAME,
                supportingText = if (dishId != null) "A dish is selected; custom name is optional" else null,
            )

            Spacer(Modifier.height(6.dp))
            LimitedTextField(
                value = servings,
                onValueChange = { servings = it },
                label = "Serving label (optional)",
                maxLength = Validation.MAX_SERVINGS_LABEL,
            )

            Spacer(Modifier.height(6.dp))
            LimitedTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
                maxLength = Validation.MAX_MEAL_NOTE,
                singleLine = false,
                minLines = 2,
            )

            if (existing != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showRepeat = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Repeat this meal on other days")
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val ts = DateUtils.nowTimestamp()
                    val entry = MealEntry(
                        id = existing?.id ?: Ids.newId(),
                        weeklyPlanId = existing?.weeklyPlanId.orEmpty(),
                        date = dateStr,
                        mealSlot = slot,
                        dishId = dishId,
                        customMealName = if (dishId != null) Validation.clean(customName) else Validation.clean(customName),
                        servingsLabel = Validation.clean(servings),
                        note = Validation.clean(note),
                        repeatedFromEntryId = existing?.repeatedFromEntryId,
                        createdAt = existing?.createdAt ?: ts,
                        updatedAt = ts,
                    )
                    vm.saveMealEntry(entry)
                    navController.popBackStack()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save meal") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDishPicker) {
        DishPickerDialog(
            dishes = data.dishes.filter { !it.archived },
            onPick = { picked -> dishId = picked.id; if (customName.isBlank()) customName = ""; showDishPicker = false },
            onDismiss = { showDishPicker = false },
            onCreateNew = { showDishPicker = false; navController.navigate(Routes.dishEdit()) },
        )
    }

    if (showDelete && existing != null) {
        ConfirmDialog(
            title = "Delete this meal?",
            message = "This meal entry will be removed from ${DateUtils.formatFull(dateStr)}.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteMealEntry(existing.id); showDelete = false; navController.popBackStack() },
            onDismiss = { showDelete = false },
        )
    }

    if (showRepeat && existing != null) {
        val weekStart = WeekUtils.weekStart(date, data.settings.firstDayOfWeek)
        val days = WeekUtils.weekDates(weekStart).map { DateUtils.toStorage(it) }
        RepeatMealDialog(
            source = existing,
            weekDays = days,
            defaultSlot = slot,
            onConfirm = { targets, targetSlot, mode ->
                vm.repeatMeal(existing, targets, targetSlot, mode)
                showRepeat = false
                navController.popBackStack()
            },
            onDismiss = { showRepeat = false },
        )
    }
}

@Composable
private fun DishPickerDialog(
    dishes: List<com.mealora.plan.model.Dish>,
    onPick: (com.mealora.plan.model.Dish) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = dishes.filter { it.name.contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareByDescending<com.mealora.plan.model.Dish> { it.favorite }.thenBy { it.name.lowercase() })

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a dish") },
        text = {
            Column {
                LimitedTextField(value = query, onValueChange = { query = it }, label = "Search dishes", maxLength = 100)
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text("No matching dishes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        filtered.forEach { dish ->
                            Surface(
                                onClick = { onPick(dish) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(dish.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(dish.category.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCreateNew) { Text("Create new dish") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RepeatMealDialog(
    source: MealEntry,
    weekDays: List<String>,
    defaultSlot: MealSlot,
    onConfirm: (List<String>, MealSlot, MergeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = remember { mutableStateOf(setOf<String>()) }
    var targetSlot by remember { mutableStateOf(defaultSlot) }
    var mode by remember { mutableStateOf(MergeMode.FillEmptyOnly) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeat meal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).height(360.dp)) {
                Text("Select days:", style = MaterialTheme.typography.bodyMedium)
                weekDays.forEach { d ->
                    val isChecked = selected.value.contains(d)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            selected.value = if (isChecked) selected.value - d else selected.value + d
                        },
                        label = { Text(DateUtils.formatFull(d)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Target slot:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealSlot.ordered.forEach { s ->
                        FilterChip(selected = targetSlot == s, onClick = { targetSlot = s }, label = { Text(s.displayName) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("If a slot already has a meal:", style = MaterialTheme.typography.bodyMedium)
                RepeatModeChips(mode = mode, onModeChange = { mode = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.value.toList(), targetSlot, mode) },
                enabled = selected.value.isNotEmpty(),
            ) { Text("Repeat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RepeatModeChips(mode: MergeMode, onModeChange: (MergeMode) -> Unit) {
    Column {
        RepeatModeChip("Keep existing (fill empty only)", mode == MergeMode.FillEmptyOnly) { onModeChange(MergeMode.FillEmptyOnly) }
        RepeatModeChip("Add another entry", mode == MergeMode.AddAll) { onModeChange(MergeMode.AddAll) }
        RepeatModeChip("Replace existing", mode == MergeMode.Replace) { onModeChange(MergeMode.Replace) }
    }
}

@Composable
private fun RepeatModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}
