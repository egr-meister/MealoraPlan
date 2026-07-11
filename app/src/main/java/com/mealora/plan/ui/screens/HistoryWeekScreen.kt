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
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Button
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
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.MergeMode
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.PlateGraphic
import com.mealora.plan.ui.label
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.ui.theme.accentColor
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Validation
import com.mealora.plan.util.WeekUtils

@Composable
fun HistoryWeekScreen(vm: PlannerViewModel, navController: NavHostController, weekStartArg: String) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val weekStart = DateUtils.parseOrNull(weekStartArg)

    if (weekStart == null) {
        Column(Modifier.fillMaxSize()) {
            MealoraTopBar(title = "Week not found", onBack = { navController.popBackStack() })
            EmptyState(Icons.Filled.EventBusy, "Week not found", "This weekly plan could not be opened.")
        }
        return
    }

    val weekDates = WeekUtils.weekDates(weekStart)
    val weekDateStrings = weekDates.map { DateUtils.toStorage(it) }
    val plan = data.weeklyPlans.firstOrNull { it.weekStartDate == weekStartArg }

    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(plan?.title.orEmpty()) }
    var showDelete by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = "Past week", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Text(WeekUtils.formatRange(weekStart), style = MaterialTheme.typography.headlineSmall)
            if (!plan?.title.isNullOrBlank()) {
                Text(plan!!.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("Read-only view", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))
            weekDates.forEachIndexed { i, date ->
                val ds = weekDateStrings[i]
                val entries = data.mealEntries.filter { it.date == ds }
                Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(DateUtils.formatFull(ds), style = MaterialTheme.typography.titleMedium)
                        MealSlot.ordered.forEach { slot ->
                            val slotEntries = entries.filter { it.mealSlot == slot }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                PlateGraphic(slot = slot, filled = slotEntries.isNotEmpty(), diameter = 26.dp)
                                Spacer(Modifier.height(0.dp).then(Modifier.padding(horizontal = 6.dp)))
                                Text("${slot.displayName}: ", style = MaterialTheme.typography.labelLarge, color = slot.accentColor())
                                Text(
                                    if (slotEntries.isEmpty()) "—" else slotEntries.joinToString(", ") { it.label(data) },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.copyWeek(weekStart, vm.selectedWeekStart(), MergeMode.FillEmptyOnly)
                }) { Text("Copy to current week") }
                OutlinedButton(onClick = { templateName = plan?.title.orEmpty(); showTemplate = true }) { Text("Save as template") }
                OutlinedButton(onClick = {
                    vm.selectDate(weekStart)
                    navController.navigate(Routes.BOARD) { popUpTo(Routes.BOARD) { inclusive = false } }
                }) { Text("Edit this week") }
                TextButton(onClick = { renameText = plan?.title.orEmpty(); showRename = true }) { Text("Rename") }
                TextButton(onClick = { showDelete = true }) { Text("Delete") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showRename) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename week") },
            text = { LimitedTextField(value = renameText, onValueChange = { renameText = it }, label = "Week title", maxLength = Validation.MAX_WEEK_TITLE) },
            confirmButton = { Button(onClick = { vm.setWeekTitle(weekStart, Validation.clean(renameText)); showRename = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    if (showTemplate) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplate = false },
            title = { Text("Save as template") },
            text = { LimitedTextField(value = templateName, onValueChange = { templateName = it }, label = "Template name", maxLength = Validation.MAX_TEMPLATE_NAME) },
            confirmButton = {
                Button(onClick = {
                    val nm = templateName.ifBlank { "Week of ${WeekUtils.formatRange(weekStart)}" }
                    vm.createTemplateFromWeek(Validation.clean(nm), "", weekStart)
                    showTemplate = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTemplate = false }) { Text("Cancel") } },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this week?",
            message = "The plan for ${WeekUtils.formatRange(weekStart)} and all its meals will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteWeek(weekStart); showDelete = false; navController.popBackStack() },
            onDismiss = { showDelete = false },
        )
    }
}
