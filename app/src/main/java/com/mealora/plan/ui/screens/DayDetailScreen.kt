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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.mealora.plan.ui.components.MealPlateCard
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.label
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Validation
import com.mealora.plan.util.WeekUtils

@Composable
fun DayDetailScreen(vm: PlannerViewModel, navController: NavHostController, dateArg: String) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val date = DateUtils.parseOrNull(dateArg)

    if (date == null) {
        Column(Modifier.fillMaxSize()) {
            MealoraTopBar(title = "Day", onBack = { navController.popBackStack() })
            EmptyState(Icons.Filled.EventBusy, "Date unavailable", "This day could not be opened.")
        }
        return
    }

    val dateStr = DateUtils.toStorage(date)
    val slotEntries = MealSlot.ordered.associateWith { slot ->
        data.mealEntries.filter { it.date == dateStr && it.mealSlot == slot }
    }
    var dayNote by remember(dateStr, data.dayNotes[dateStr]) { mutableStateOf(data.dayNotes[dateStr].orEmpty()) }
    var showCopy by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        MealoraTopBar(title = DateUtils.weekdayFull(date), onBack = { navController.popBackStack() })
        Column(Modifier.padding(horizontal = 14.dp)) {
            Text(DateUtils.formatFull(dateStr), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            MealSlot.ordered.forEach { slot ->
                MealPlateCard(
                    slot = slot,
                    entries = slotEntries[slot].orEmpty(),
                    labelFor = { it.label(data) },
                    onAdd = { navController.navigate(Routes.mealEdit(dateStr, slot.name)) },
                    onEntryClick = { navController.navigate(Routes.mealEdit(dateStr, slot.name, it.id)) },
                    modifier = Modifier.padding(vertical = 5.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Day note")
            LimitedTextField(
                value = dayNote,
                onValueChange = { dayNote = it },
                label = "Note for this day",
                maxLength = Validation.MAX_DAY_NOTE,
                singleLine = false,
                minLines = 2,
            )
            OutlinedButton(onClick = { vm.setDayNote(dateStr, Validation.clean(dayNote)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save day note")
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showCopy = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(2.dp))
                    Text("Copy Day")
                }
                OutlinedButton(onClick = { showClear = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(2.dp))
                    Text("Clear Day")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCopy) {
        val weekStart = WeekUtils.weekStart(date, data.settings.firstDayOfWeek)
        val targets = WeekUtils.weekDates(weekStart).map { DateUtils.toStorage(it) }.filter { it != dateStr }
        CopyDayDialog(
            targets = targets,
            onConfirm = { target, mode -> vm.copyDay(dateStr, target, mode); showCopy = false },
            onDismiss = { showCopy = false },
        )
    }

    if (showClear) {
        ConfirmDialog(
            title = "Clear this day?",
            message = "All meals planned on ${DateUtils.formatFull(dateStr)} will be removed. This cannot be undone.",
            confirmLabel = "Clear Day",
            onConfirm = { vm.clearDay(dateStr); showClear = false },
            onDismiss = { showClear = false },
        )
    }
}

@Composable
private fun CopyDayDialog(
    targets: List<String>,
    onConfirm: (String, MergeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTarget by remember { mutableStateOf(targets.firstOrNull().orEmpty()) }
    var mode by remember { mutableStateOf(MergeMode.AddAll) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy day to…") },
        text = {
            Column {
                Text("Choose a target day:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                targets.forEach { t ->
                    FilterChip(
                        selected = selectedTarget == t,
                        onClick = { selectedTarget = t },
                        label = { Text(DateUtils.formatFull(t)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("How to handle existing meals:", style = MaterialTheme.typography.bodyMedium)
                MergeModeChips(mode = mode, onModeChange = { mode = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTarget, mode) },
                enabled = selectedTarget.isNotBlank(),
            ) { Text("Copy") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun MergeModeChips(mode: MergeMode, onModeChange: (MergeMode) -> Unit) {
    Column {
        MergeChip("Add copied meals", mode == MergeMode.AddAll) { onModeChange(MergeMode.AddAll) }
        MergeChip("Fill empty slots only", mode == MergeMode.FillEmptyOnly) { onModeChange(MergeMode.FillEmptyOnly) }
        MergeChip("Replace target meals", mode == MergeMode.Replace) { onModeChange(MergeMode.Replace) }
    }
}

@Composable
private fun MergeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}
