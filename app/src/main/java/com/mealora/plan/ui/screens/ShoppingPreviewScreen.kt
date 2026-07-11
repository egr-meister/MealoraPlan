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
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.mealora.plan.model.ShoppingGenerationMode
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.WeekUtils

@Composable
fun ShoppingPreviewScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val board by vm.board.collectAsStateWithLifecycle()

    val weekDates = WeekUtils.weekDates(board.weekStart).map { DateUtils.toStorage(it) }
    val dishesById = data.dishes.associateBy { it.id }
    val weekEntries = data.mealEntries.filter { weekDates.contains(it.date) && it.dishId != null }
    val linkedDishes = weekEntries.mapNotNull { e -> dishesById[e.dishId] }.distinctBy { it.id }
    val ingredientPairs = linkedDishes.flatMap { d -> d.ingredients.map { d to it } }

    var included by remember(ingredientPairs.size) {
        mutableStateOf(ingredientPairs.filter { it.second.addToShoppingByDefault }.map { it.second.id }.toSet())
    }
    var mode by remember { mutableStateOf(ShoppingGenerationMode.AddMissing) }

    val candidates = remember(included, weekEntries) { vm.previewShopping(weekEntries, included) }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = "Generate shopping list", onBack = { navController.popBackStack() })

        if (ingredientPairs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.RemoveShoppingCart,
                title = "Nothing to generate",
                message = "This week's meals have no linked dishes with ingredients. Custom meal names are never turned into ingredients.",
            )
            return
        }

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Text(
                "Only manually entered dish ingredients are used. Quantities are kept as your own text and never combined or converted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            SectionLabel("Ingredients to include")
            linkedDishes.forEach { dish ->
                if (dish.ingredients.isNotEmpty()) {
                    Text(dish.name, style = MaterialTheme.typography.titleMedium)
                    dish.ingredients.forEach { ing ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = included.contains(ing.id),
                                onCheckedChange = { on ->
                                    included = if (on) included + ing.id else included - ing.id
                                },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(ing.name.ifBlank { "(unnamed)" }, style = MaterialTheme.typography.bodyLarge)
                                if (ing.quantityLabel.isNotBlank()) {
                                    Text(ing.quantityLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("Preview (${candidates.size} grouped items)")
            if (candidates.isEmpty()) {
                Text("No ingredients selected.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                candidates.forEach { c ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.displayName, style = MaterialTheme.typography.bodyLarge)
                            val sub = buildString {
                                append(c.category.displayName)
                                if (c.dishesNote.isNotBlank()) append(" · ${c.dishesNote}")
                            }
                            Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (c.quantityLabels.isNotEmpty()) {
                                Text("Quantities: ${c.quantityLabels.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionLabel("Save mode")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = mode == ShoppingGenerationMode.AddMissing, onClick = { mode = ShoppingGenerationMode.AddMissing }, label = { Text("Add missing") })
                FilterChip(selected = mode == ShoppingGenerationMode.ReplaceGenerated, onClick = { mode = ShoppingGenerationMode.ReplaceGenerated }, label = { Text("Replace generated") })
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    vm.generateShopping(candidates, board.weekStart, mode)
                    navController.popBackStack()
                },
                enabled = candidates.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save to shopping list") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
