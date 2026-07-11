package com.mealora.plan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.TemplateMealEntry
import com.mealora.plan.model.WeeklyTemplate
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.theme.accentColor
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Ids
import com.mealora.plan.util.Validation
import com.mealora.plan.util.WeekUtils

@Composable
fun TemplateEditScreen(vm: PlannerViewModel, navController: NavHostController, templateIdArg: String) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val existing = remember(templateIdArg, data.weeklyTemplates) {
        if (templateIdArg.isBlank()) null else data.weeklyTemplates.firstOrNull { it.id == templateIdArg }
    }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    val entries = remember { mutableStateListOf<TemplateMealEntry>().apply { existing?.entries?.let { addAll(it) } } }

    var addingAt by remember { mutableStateOf<Pair<Int, MealSlot>?>(null) }

    val dayLabels = remember { (0 until WeekUtils.DAYS_IN_WEEK).map { "Day ${it + 1}" } }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = if (existing == null) "New template" else "Edit template", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            LimitedTextField(value = name, onValueChange = { name = it }, label = "Template name", maxLength = Validation.MAX_TEMPLATE_NAME, isError = !Validation.isNonBlank(name))
            Spacer(Modifier.height(4.dp))
            LimitedTextField(value = description, onValueChange = { description = it }, label = "Description (optional)", maxLength = Validation.MAX_TEMPLATE_DESCRIPTION, singleLine = false, minLines = 2)

            Spacer(Modifier.height(10.dp))
            dayLabels.forEachIndexed { dayIndex, label ->
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                MealSlot.ordered.forEach { slot ->
                    val slotEntries = entries.filter { it.dayIndex == dayIndex && it.mealSlot == slot }
                    Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(slot.displayName, style = MaterialTheme.typography.labelLarge, color = slot.accentColor(), modifier = Modifier.weight(1f))
                                IconButton(onClick = { addingAt = dayIndex to slot }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add ${slot.displayName} to $label")
                                }
                            }
                            if (slotEntries.isEmpty()) {
                                Text("Empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                slotEntries.forEach { e ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val nm = e.dishId?.let { id -> data.dishes.firstOrNull { it.id == id }?.name } ?: e.customMealName
                                        Text(nm.ifBlank { "(unnamed)" }, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { entries.remove(e) }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val ts = DateUtils.nowTimestamp()
                    val template = WeeklyTemplate(
                        id = existing?.id ?: Ids.newId(),
                        name = Validation.clean(name),
                        description = Validation.clean(description),
                        entries = entries.toList(),
                        createdAt = existing?.createdAt ?: ts,
                        updatedAt = ts,
                    )
                    vm.saveTemplate(template)
                    navController.popBackStack()
                },
                enabled = Validation.isNonBlank(name),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save template") }
            Spacer(Modifier.height(24.dp))
        }
    }

    addingAt?.let { (dayIndex, slot) ->
        AddTemplateEntryDialog(
            dishes = data.dishes.filter { !it.archived },
            onConfirm = { dishId, custom, servings, note ->
                entries.add(
                    TemplateMealEntry(
                        id = Ids.newId(),
                        dayIndex = dayIndex,
                        mealSlot = slot,
                        dishId = dishId,
                        customMealName = custom,
                        servingsLabel = servings,
                        note = note,
                    ),
                )
                addingAt = null
            },
            onDismiss = { addingAt = null },
        )
    }
}

@Composable
private fun AddTemplateEntryDialog(
    dishes: List<com.mealora.plan.model.Dish>,
    onConfirm: (String?, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var dishId by remember { mutableStateOf<String?>(null) }
    var custom by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    val filtered = dishes.filter { it.name.contains(query.trim(), ignoreCase = true) }.sortedBy { it.name.lowercase() }
    val canSave = dishId != null || Validation.isNonBlank(custom)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to template position") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).height(380.dp)) {
                SectionLabel("Choose a dish (optional)")
                LimitedTextField(value = query, onValueChange = { query = it }, label = "Search dishes", maxLength = 100)
                filtered.take(20).forEach { dish ->
                    FilterChip(
                        selected = dishId == dish.id,
                        onClick = { dishId = if (dishId == dish.id) null else dish.id },
                        label = { Text(dish.name) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                SectionLabel("Or custom meal name")
                LimitedTextField(value = custom, onValueChange = { custom = it }, label = "Custom name", maxLength = Validation.MAX_CUSTOM_MEAL_NAME)
                LimitedTextField(value = servings, onValueChange = { servings = it }, label = "Serving label (optional)", maxLength = Validation.MAX_SERVINGS_LABEL)
                LimitedTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", maxLength = Validation.MAX_MEAL_NOTE, singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(dishId, Validation.clean(custom), Validation.clean(servings), Validation.clean(note)) }, enabled = canSave) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
