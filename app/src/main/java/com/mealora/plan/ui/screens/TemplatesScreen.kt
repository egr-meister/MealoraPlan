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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewModule
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
import com.mealora.plan.model.MergeMode
import com.mealora.plan.model.WeeklyTemplate
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.WeekUtils

@Composable
fun TemplatesScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val board by vm.board.collectAsStateWithLifecycle()

    var applyTarget by remember { mutableStateOf<WeeklyTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<WeeklyTemplate?>(null) }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(
            title = "Templates",
            actions = {
                TextButton(onClick = { navController.navigate(Routes.templateEdit()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "New template")
                    Text("New")
                }
            },
        )

        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.createTemplateFromWeek("Week of ${WeekUtils.formatRange(board.weekStart)}", "", board.weekStart) },
                modifier = Modifier.weight(1f),
            ) { Text("From current week") }
            OutlinedButton(onClick = { navController.navigate(Routes.templateEdit()) }, modifier = Modifier.weight(1f)) {
                Text("Empty template")
            }
        }

        if (data.weeklyTemplates.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ViewModule,
                title = "No templates yet",
                message = "Save a week as a template, or build one from scratch. Templates hold organizational meal positions you can reuse.",
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                items(data.weeklyTemplates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        uniqueDishCount = template.entries.mapNotNull { it.dishId }.distinct().size,
                        onEdit = { navController.navigate(Routes.templateEdit(template.id)) },
                        onApply = { applyTarget = template },
                        onDuplicate = { vm.duplicateTemplate(template.id) },
                        onDelete = { deleteTarget = template },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    applyTarget?.let { template ->
        ApplyTemplateDialog(
            template = template,
            currentWeekLabel = WeekUtils.formatRange(board.weekStart),
            nextWeekLabel = WeekUtils.formatRange(WeekUtils.nextWeekStart(board.weekStart)),
            onConfirm = { toNext, mode ->
                val target = if (toNext) WeekUtils.nextWeekStart(board.weekStart) else board.weekStart
                vm.applyTemplate(template.id, target, mode)
                applyTarget = null
            },
            onDismiss = { applyTarget = null },
        )
    }

    deleteTarget?.let { template ->
        ConfirmDialog(
            title = "Delete template?",
            message = "\"${template.name.ifBlank { "Untitled template" }}\" will be permanently removed. Weekly plans already created from it are not affected.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteTemplate(template.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TemplateCard(
    template: WeeklyTemplate,
    uniqueDishCount: Int,
    onEdit: () -> Unit,
    onApply: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(template.name.ifBlank { "Untitled template" }, style = MaterialTheme.typography.titleMedium)
            if (template.description.isNotBlank()) {
                Text(template.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${template.entries.size} planned slots · $uniqueDishCount unique dishes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply) { Text("Apply") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDuplicate) { Text("Duplicate") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun ApplyTemplateDialog(
    template: WeeklyTemplate,
    currentWeekLabel: String,
    nextWeekLabel: String,
    onConfirm: (Boolean, MergeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var toNext by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(MergeMode.FillEmptyOnly) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply template") },
        text = {
            Column {
                Text("Target week:", style = MaterialTheme.typography.bodyMedium)
                FilterChip(selected = !toNext, onClick = { toNext = false }, label = { Text("Current: $currentWeekLabel") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                FilterChip(selected = toNext, onClick = { toNext = true }, label = { Text("Next: $nextWeekLabel") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                Spacer(Modifier.height(8.dp))
                Text("How to apply:", style = MaterialTheme.typography.bodyMedium)
                FilterChip(selected = mode == MergeMode.FillEmptyOnly, onClick = { mode = MergeMode.FillEmptyOnly }, label = { Text("Fill empty slots") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                FilterChip(selected = mode == MergeMode.AddAll, onClick = { mode = MergeMode.AddAll }, label = { Text("Merge all entries") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                FilterChip(selected = mode == MergeMode.Replace, onClick = { mode = MergeMode.Replace }, label = { Text("Replace entire week") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
            }
        },
        confirmButton = { Button(onClick = { onConfirm(toNext, mode) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
