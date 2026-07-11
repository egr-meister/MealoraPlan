package com.mealora.plan.ui.screens

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.MealEntry
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Ids

@Composable
fun DishDetailScreen(vm: PlannerViewModel, navController: NavHostController, dishIdArg: String) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val dish = remember(dishIdArg, data.dishes) { data.dishes.firstOrNull { it.id == dishIdArg } }

    if (dish == null) {
        Column(Modifier.fillMaxSize()) {
            MealoraTopBar(title = "Dish", onBack = { navController.popBackStack() })
            EmptyState(Icons.Filled.SearchOff, "Dish not found", "This dish may have been deleted.")
        }
        return
    }

    var showDelete by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var deleteBlocked by remember { mutableStateOf(false) }

    val today = DateUtils.today()
    val usages: List<MealEntry> = data.mealEntries.filter { it.dishId == dish.id }
    val timesPlanned = usages.size
    val sortedDates = usages.mapNotNull { DateUtils.parseOrNull(it.date) }.sorted()
    val lastPlanned = sortedDates.lastOrNull { !it.isAfter(today) }
    val upcoming = sortedDates.filter { it.isAfter(today) }.distinct()
    val slotsUsed = usages.map { it.mealSlot.displayName }.distinct()

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(
            title = "Dish",
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { vm.setDishFavorite(dish.id, !dish.favorite) }) {
                    Icon(
                        if (dish.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Toggle favorite",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { navController.navigate(Routes.dishEdit(dish.id)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit dish")
                }
            },
        )
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Text(dish.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(dish.category.displayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (dish.archived) {
                Text("Archived", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (dish.defaultServingsLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Default serving: ${dish.defaultServingsLabel}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Ingredients")
            if (dish.ingredients.isEmpty()) {
                Text("No ingredients entered.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dish.ingredients.forEach { ing ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(12.dp)) {
                            Text(ing.name, modifier = Modifier.weight(1f))
                            if (ing.quantityLabel.isNotBlank()) Text(ing.quantityLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (dish.preparationNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                SectionLabel("Preparation note")
                Text(dish.preparationNote, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Usage")
            Text("Planned $timesPlanned time${if (timesPlanned == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium)
            Text("Last planned: ${lastPlanned?.let { DateUtils.formatFull(DateUtils.toStorage(it)) } ?: "—"}", style = MaterialTheme.typography.bodyMedium)
            Text("Meal slots used: ${if (slotsUsed.isEmpty()) "—" else slotsUsed.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            if (upcoming.isNotEmpty()) {
                Text("Upcoming: ${upcoming.take(5).joinToString(", ") { DateUtils.formatMedium(it) }}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val ts = DateUtils.nowTimestamp()
                    val entry = MealEntry(
                        id = Ids.newId(),
                        weeklyPlanId = "",
                        date = DateUtils.toStorage(vm.selectedDate.value),
                        mealSlot = data.settings.defaultMealSlot,
                        dishId = dish.id,
                        customMealName = "",
                        servingsLabel = dish.defaultServingsLabel,
                        note = "",
                        repeatedFromEntryId = null,
                        createdAt = ts,
                        updatedAt = ts,
                    )
                    vm.saveMealEntry(entry)
                    navController.navigate(Routes.BOARD) { popUpTo(Routes.BOARD) { inclusive = false } }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to selected day (${DateUtils.formatMedium(vm.selectedDate.value)})") }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showArchive = true }, modifier = Modifier.weight(1f)) {
                    Icon(if (dish.archived) Icons.Filled.Unarchive else Icons.Filled.Archive, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(if (dish.archived) "Restore" else "Archive")
                }
                OutlinedButton(onClick = {
                    if (vm.isDishUnused(dish.id)) showDelete = true else deleteBlocked = true
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(2.dp))
                    Text("Delete")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showArchive) {
        ConfirmDialog(
            title = if (dish.archived) "Restore dish?" else "Archive dish?",
            message = if (dish.archived) "This dish will appear in your active list again." else "Archived dishes stay in past meal history but are hidden from the active list.",
            confirmLabel = if (dish.archived) "Restore" else "Archive",
            onConfirm = {
                if (dish.archived) vm.restoreDish(dish.id) else vm.archiveDish(dish.id)
                showArchive = false
            },
            onDismiss = { showArchive = false },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete dish?",
            message = "This dish is not used in any plan or template and will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteDishIfUnused(dish.id) { deleted -> if (deleted) navController.popBackStack() }
                showDelete = false
            },
            onDismiss = { showDelete = false },
        )
    }

    if (deleteBlocked) {
        ConfirmDialog(
            title = "Can't delete this dish",
            message = "This dish is used by meal entries or templates. Archive it instead to keep your history intact.",
            confirmLabel = "OK",
            dismissLabel = "Close",
            onConfirm = { deleteBlocked = false },
            onDismiss = { deleteBlocked = false },
        )
    }
}
