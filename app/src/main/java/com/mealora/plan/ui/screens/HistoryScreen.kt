package com.mealora.plan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FilterChip
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
import com.mealora.plan.model.AppData
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.WeekUtils
import java.time.LocalDate

private data class HistoryRow(
    val weekStart: LocalDate,
    val weekStartStr: String,
    val rangeLabel: String,
    val title: String,
    val entryCount: Int,
    val uniqueDishes: Int,
    val shoppingCount: Int,
    val dishNames: List<String>,
)

@Composable
fun HistoryScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var monthFilter by remember { mutableStateOf<String?>(null) }

    val rows = remember(data) { buildHistory(data) }
    val months = remember(rows) { rows.map { it.weekStartStr.substring(0, 7) }.distinct().sortedDescending() }

    val filtered = rows.filter { row ->
        (monthFilter == null || row.weekStartStr.startsWith(monthFilter!!)) &&
            (query.isBlank() || row.title.contains(query, true) || row.dishNames.any { it.contains(query, true) })
    }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = "Menu history")
        Column(Modifier.padding(horizontal = 14.dp)) {
            LimitedTextField(value = query, onValueChange = { query = it }, label = "Search by dish or week title", maxLength = 100)
            if (months.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = monthFilter == null, onClick = { monthFilter = null }, label = { Text("All months") })
                    months.forEach { m ->
                        FilterChip(selected = monthFilter == m, onClick = { monthFilter = if (monthFilter == m) null else m }, label = { Text(m) })
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.History,
                title = "No past weeks yet",
                message = "Weeks you plan will appear here in reverse chronological order.",
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                items(filtered, key = { it.weekStartStr }) { row ->
                    HistoryCard(
                        row = row,
                        onOpen = { navController.navigate(Routes.historyWeek(row.weekStartStr)) },
                        onCopy = { vm.copyWeek(row.weekStart, vm.selectedWeekStart(), com.mealora.plan.model.MergeMode.FillEmptyOnly) },
                        onSaveTemplate = { vm.createTemplateFromWeek(row.title.ifBlank { "Week of ${row.rangeLabel}" }, "", row.weekStart) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryCard(row: HistoryRow, onOpen: () -> Unit, onCopy: () -> Unit, onSaveTemplate: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(row.rangeLabel, style = MaterialTheme.typography.titleMedium)
            if (row.title.isNotBlank()) Text(row.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "${row.entryCount} meals · ${row.uniqueDishes} unique dishes · ${row.shoppingCount} shopping items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onCopy) { Text("Copy to current") }
                TextButton(onClick = onSaveTemplate) { Text("Save as template") }
            }
        }
    }
}

private fun buildHistory(data: AppData): List<HistoryRow> {
    // Weeks known from stored plans plus any week that has meal entries.
    val planStarts = data.weeklyPlans.mapNotNull { DateUtils.parseOrNull(it.weekStartDate) }
    val entryWeekStarts = data.mealEntries.mapNotNull { DateUtils.parseOrNull(it.date) }
        .map { WeekUtils.weekStart(it, data.settings.firstDayOfWeek) }
    val allStarts = (planStarts + entryWeekStarts).distinct()

    return allStarts.map { start ->
        val weekDates = WeekUtils.weekDates(start).map { DateUtils.toStorage(it) }
        val weekEntries = data.mealEntries.filter { weekDates.contains(it.date) }
        val startStr = DateUtils.toStorage(start)
        val plan = data.weeklyPlans.firstOrNull { it.weekStartDate == startStr }
        val dishNames = weekEntries.map { e ->
            e.dishId?.let { id -> data.dishes.firstOrNull { it.id == id }?.name } ?: e.customMealName
        }.filter { it.isNotBlank() }
        HistoryRow(
            weekStart = start,
            weekStartStr = startStr,
            rangeLabel = WeekUtils.formatRange(start),
            title = plan?.title.orEmpty(),
            entryCount = weekEntries.size,
            uniqueDishes = dishNames.distinct().size,
            shoppingCount = data.shoppingItems.count { it.weekStartDate == startStr },
            dishNames = dishNames,
        )
    }.sortedByDescending { it.weekStart }
}
