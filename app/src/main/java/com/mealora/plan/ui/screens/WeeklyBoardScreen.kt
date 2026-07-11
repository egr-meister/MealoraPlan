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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.MealSlot
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.DayRibbon
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealPlateCard
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.components.ShoppingBasketStrip
import com.mealora.plan.ui.components.WeekNavigator
import com.mealora.plan.ui.components.WeeklyOverview
import com.mealora.plan.ui.label
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Validation

@Composable
fun WeeklyBoardScreen(vm: PlannerViewModel, navController: NavHostController) {
    val board by vm.board.collectAsStateWithLifecycle()
    val data by vm.appData.collectAsStateWithLifecycle()
    val prompt by vm.prompt.collectAsStateWithLifecycle()

    var showTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        // Top bar row: title + quick actions.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Mealora Plan",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { navController.navigate(Routes.STATS) }) {
                Icon(Icons.Filled.BarChart, contentDescription = "Statistics")
            }
            IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        WeekNavigator(
            rangeLabel = board.rangeLabel,
            isCurrentWeek = board.isCurrentWeek,
            onPrevious = { vm.previousWeek() },
            onNext = { vm.nextWeek() },
            onToday = { vm.goToCurrentWeek() },
        )

        Spacer(Modifier.height(8.dp))
        DayRibbon(days = board.daySummaries, onSelect = { vm.selectDate(it.date) })

        // In-app planning prompt.
        prompt?.let { p ->
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(p.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            // All prompt primary actions lead to planning the selected/today week.
                            vm.goToCurrentWeek()
                            when (p.type.name) {
                                "NoShoppingList" -> navController.navigate(Routes.SHOPPING)
                                "TemplateAvailable" -> navController.navigate(Routes.TEMPLATES)
                                else -> {}
                            }
                        }) { Text(p.primaryLabel) }
                        TextButton(onClick = { vm.dismissPrompt(p) }) { Text("Not Now") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                DateUtils.formatFull(board.selectedDateString),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { navController.navigate(Routes.day(board.selectedDateString)) }) {
                Text("Day detail")
            }
        }

        if (board.weekIsEmpty) {
            Spacer(Modifier.height(4.dp))
            Text(
                "No meals planned this week.\nChoose a plate to add your first meal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(6.dp))
        // The dining board: four plate cards.
        MealSlot.ordered.forEach { slot ->
            MealPlateCard(
                slot = slot,
                entries = board.slotEntries[slot].orEmpty(),
                labelFor = { it.label(data) },
                onAdd = { navController.navigate(Routes.mealEdit(board.selectedDateString, slot.name)) },
                onEntryClick = { navController.navigate(Routes.mealEdit(board.selectedDateString, slot.name, it.id)) },
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }

        Spacer(Modifier.height(14.dp))
        ShoppingBasketStrip(
            itemCount = board.weekShoppingCount,
            remaining = data.shoppingItems.count { it.weekStartDate == DateUtils.toStorage(board.weekStart) && !it.checked },
            onClick = { navController.navigate(Routes.SHOPPING) },
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("Weekly overview")
        WeeklyOverview(
            days = board.daySummaries,
            onOpenDay = { vm.selectDate(it.date); navController.navigate(Routes.day(it.dateString)) },
        )

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { templateName = ""; showTemplateDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.height(18.dp))
            Spacer(Modifier.height(4.dp))
            Text("Save week as template")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showTemplateDialog) {
        val weekStart = board.weekStart
        ConfirmSaveTemplateDialog(
            name = templateName,
            onNameChange = { templateName = it },
            onConfirm = {
                if (Validation.isNonBlank(templateName)) {
                    vm.createTemplateFromWeek(Validation.clean(templateName), "", weekStart)
                }
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false },
        )
    }
}

@Composable
private fun ConfirmSaveTemplateDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save week as template") },
        text = {
            Column {
                Text(
                    "Meals from this week become reusable template positions.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                LimitedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Template name",
                    maxLength = Validation.MAX_TEMPLATE_NAME,
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
