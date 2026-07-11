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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.mealora.plan.model.Dish
import com.mealora.plan.model.DishCategory
import com.mealora.plan.model.DishIngredient
import com.mealora.plan.model.ShoppingCategory
import com.mealora.plan.ui.Copy
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.DisclaimerBanner
import com.mealora.plan.ui.components.LimitedTextField
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.util.DateUtils
import com.mealora.plan.util.Ids
import com.mealora.plan.util.Validation

private data class EditableIngredient(
    val id: String,
    var name: String,
    var quantity: String,
    var category: ShoppingCategory,
    var addByDefault: Boolean,
    val createdAt: String,
)

@Composable
fun DishEditScreen(vm: PlannerViewModel, navController: NavHostController, dishIdArg: String) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val existing = remember(dishIdArg, data.dishes) {
        if (dishIdArg.isBlank()) null else data.dishes.firstOrNull { it.id == dishIdArg }
    }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: DishCategory.MainDish) }
    var prep by remember { mutableStateOf(existing?.preparationNote.orEmpty()) }
    var serving by remember { mutableStateOf(existing?.defaultServingsLabel.orEmpty()) }
    var favorite by remember { mutableStateOf(existing?.favorite ?: false) }
    val ingredients = remember {
        mutableStateListOf<EditableIngredient>().apply {
            existing?.ingredients?.forEach {
                add(EditableIngredient(it.id.ifBlank { Ids.newId() }, it.name, it.quantityLabel, it.shoppingCategory, it.addToShoppingByDefault, it.createdAt))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = if (existing == null) "New dish" else "Edit dish", onBack = { navController.popBackStack() })
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        ) {
            LimitedTextField(value = name, onValueChange = { name = it }, label = "Dish name", maxLength = Validation.MAX_DISH_NAME, isError = !Validation.isNonBlank(name))

            Spacer(Modifier.height(10.dp))
            SectionLabel("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DishCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.displayName) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Ingredients", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    ingredients.add(EditableIngredient(Ids.newId(), "", "", ShoppingCategory.Other, true, DateUtils.nowTimestamp()))
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.height(16.dp))
                    Text("Add")
                }
            }
            if (ingredients.isEmpty()) {
                Text("No ingredients yet. Ingredients are optional and entered manually.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ingredients.forEachIndexed { index, ing ->
                IngredientEditor(
                    ingredient = ing,
                    onChange = { ingredients[index] = it },
                    onRemove = { ingredients.removeAt(index) },
                )
            }

            Spacer(Modifier.height(12.dp))
            LimitedTextField(value = serving, onValueChange = { serving = it }, label = "Default serving label (optional)", maxLength = Validation.MAX_SERVINGS_LABEL)

            Spacer(Modifier.height(6.dp))
            LimitedTextField(value = prep, onValueChange = { prep = it }, label = "Preparation note (optional)", maxLength = Validation.MAX_PREPARATION_NOTE, singleLine = false, minLines = 3)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Favorite", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = favorite, onCheckedChange = { favorite = it })
            }

            Spacer(Modifier.height(10.dp))
            DisclaimerBanner(Copy.DISH_FORM_DISCLAIMER)

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val ts = DateUtils.nowTimestamp()
                    val dish = Dish(
                        id = existing?.id ?: Ids.newId(),
                        name = Validation.clean(name),
                        category = category,
                        ingredients = ingredients
                            .filter { Validation.isNonBlank(it.name) }
                            .map {
                                DishIngredient(
                                    id = it.id,
                                    name = Validation.clean(it.name),
                                    quantityLabel = Validation.clean(it.quantity),
                                    shoppingCategory = it.category,
                                    addToShoppingByDefault = it.addByDefault,
                                    createdAt = it.createdAt.ifBlank { ts },
                                    updatedAt = ts,
                                )
                            },
                        preparationNote = Validation.clean(prep),
                        defaultServingsLabel = Validation.clean(serving),
                        favorite = favorite,
                        archived = existing?.archived ?: false,
                        createdAt = existing?.createdAt ?: ts,
                        updatedAt = ts,
                    )
                    vm.saveDish(dish)
                    navController.popBackStack()
                },
                enabled = Validation.isNonBlank(name),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save dish") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IngredientEditor(
    ingredient: EditableIngredient,
    onChange: (EditableIngredient) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ingredient", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove ingredient") }
            }
            LimitedTextField(value = ingredient.name, onValueChange = { onChange(ingredient.copy(name = it)) }, label = "Name", maxLength = Validation.MAX_INGREDIENT_NAME)
            Spacer(Modifier.height(4.dp))
            LimitedTextField(value = ingredient.quantity, onValueChange = { onChange(ingredient.copy(quantity = it)) }, label = "Quantity (free text, e.g. 1 pack)", maxLength = Validation.MAX_QUANTITY_LABEL)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShoppingCategory.entries.forEach { c ->
                    FilterChip(selected = ingredient.category == c, onClick = { onChange(ingredient.copy(category = c)) }, label = { Text(c.displayName) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ingredient.addByDefault, onCheckedChange = { onChange(ingredient.copy(addByDefault = it)) })
                Text("Add to shopping list by default", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
