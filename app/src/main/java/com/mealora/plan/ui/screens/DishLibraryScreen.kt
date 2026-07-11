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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.Dish
import com.mealora.plan.model.DishCategory
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.EmptyState
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.navigation.Routes

@Composable
fun DishLibraryScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<DishCategory?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }

    val filtered = remember(data.dishes, query, category, favoritesOnly, showArchived) {
        data.dishes
            .filter { it.archived == showArchived }
            .filter { favoritesOnly.not() || it.favorite }
            .filter { category == null || it.category == category }
            .filter { d ->
                val q = query.trim()
                q.isEmpty() || d.name.contains(q, ignoreCase = true) ||
                    d.ingredients.any { it.name.contains(q, ignoreCase = true) }
            }
            .sortedWith(compareByDescending<Dish> { it.favorite }.thenBy { it.name.lowercase() })
    }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(
            title = "Dishes",
            actions = {
                TextButton(onClick = { navController.navigate(Routes.dishEdit()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add dish")
                    Text("Add")
                }
            },
        )
        Column(Modifier.padding(horizontal = 14.dp)) {
            LimitedTextField(value = query, onValueChange = { query = it }, label = "Search dishes and ingredients", maxLength = 100)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Favorites") })
                FilterChip(selected = showArchived, onClick = { showArchived = !showArchived }, label = { Text("Archived") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                DishCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c.displayName) })
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = if (showArchived) "No archived dishes" else "No dishes yet",
                message = if (showArchived) "Archived dishes will appear here." else "Create a personal dish to reuse across your weekly plans.",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                items(filtered, key = { it.id }) { dish ->
                    DishRow(dish = dish, onClick = { navController.navigate(Routes.dishDetail(dish.id)) })
                }
            }
        }
    }
}

@Composable
private fun DishRow(dish: Dish, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dish.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(dish.category.displayName)
                        if (dish.ingredients.isNotEmpty()) append(" · ${dish.ingredients.size} ingredients")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (dish.favorite) {
                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
            }
        }
    }
}
