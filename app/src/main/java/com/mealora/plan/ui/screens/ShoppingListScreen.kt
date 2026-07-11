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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.model.ShoppingItem
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Validation
import com.mealora.plan.util.WeekUtils

@Composable
fun ShoppingListScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val board by vm.board.collectAsStateWithLifecycle()
    val weekStartStr = DateUtils.toStorage(board.weekStart)

    var categoryFilter by remember { mutableStateOf<ShoppingCategory?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val weekItems = data.shoppingItems.filter { it.weekStartDate == weekStartStr }
        .filter { categoryFilter == null || it.category == categoryFilter }
    val unchecked = weekItems.filter { !it.checked }.sortedBy { it.category.ordinal }
    val checked = weekItems.filter { it.checked }.sortedBy { it.category.ordinal }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(
            title = "Shopping",
            actions = {
                TextButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add item")
                    Text("Add")
                }
            },
        )
        Column(Modifier.padding(horizontal = 14.dp)) {
            // Week selector (paper-slip styling).
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.previousWeek() }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week") }
                    Text(WeekUtils.formatRange(board.weekStart), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { vm.nextWeek() }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next week") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate(Routes.SHOPPING_PREVIEW) }, modifier = Modifier.weight(1f)) {
                    Text("Generate from week")
                }
                OutlinedButton(onClick = { vm.clearCheckedShopping() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlaylistAddCheck, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text("Clear checked")
                }
            }

            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All") })
                ShoppingCategory.entries.forEach { c ->
                    FilterChip(selected = categoryFilter == c, onClick = { categoryFilter = if (categoryFilter == c) null else c }, label = { Text(c.displayName) })
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            if (weekItems.isEmpty()) {
                item {
                    Text(
                        "No shopping items for this week yet. Add a custom item, or generate a list from your planned meals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            if (unchecked.isNotEmpty()) {
                item { SectionLabel("To buy (${unchecked.size})") }
                items(unchecked, key = { it.id }) { ShoppingRow(it, vm) }
            }
            if (checked.isNotEmpty()) {
                item { SectionLabel("Bought (${checked.size})") }
                items(checked, key = { it.id }) { ShoppingRow(it, vm) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAdd) {
        AddShoppingItemDialog(
            onConfirm = { title, qty, cat, note ->
                vm.addCustomShoppingItem(title, qty, cat, board.weekStart, note)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, vm: PlannerViewModel) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.checked, onCheckedChange = { vm.setShoppingChecked(item.id, it) })
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                val sub = buildString {
                    append(item.category.displayName)
                    if (item.quantityLabel.isNotBlank()) append(" · ${item.quantityLabel}")
                    if (item.note.isNotBlank()) append(" · ${item.note}")
                }
                Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.deleteShoppingItem(item.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddShoppingItemDialog(
    onConfirm: (String, String, ShoppingCategory, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(ShoppingCategory.Other) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add shopping item") },
        text = {
            Column {
                LimitedTextField(value = title, onValueChange = { title = it }, label = "Item", maxLength = Validation.MAX_SHOPPING_TITLE)
                Spacer(Modifier.height(4.dp))
                LimitedTextField(value = qty, onValueChange = { qty = it }, label = "Quantity (free text)", maxLength = Validation.MAX_QUANTITY_LABEL)
                Spacer(Modifier.height(4.dp))
                LimitedTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", maxLength = Validation.MAX_SHOPPING_NOTE)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShoppingCategory.entries.forEach { c ->
                        FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c.displayName) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Validation.clean(title), Validation.clean(qty), cat, Validation.clean(note)) }, enabled = Validation.isNonBlank(title)) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
