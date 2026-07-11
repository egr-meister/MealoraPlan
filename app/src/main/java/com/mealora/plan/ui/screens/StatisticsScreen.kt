package com.mealora.plan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mealora.plan.model.MealSlot
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel
import com.mealora.plan.ui.theme.accentColor
import com.mealora.plan.util.WeekUtils

@Composable
fun StatisticsScreen(vm: PlannerViewModel, navController: NavHostController) {
    // Recompute whenever underlying data changes.
    val data by vm.appData.collectAsStateWithLifecycle()
    val board by vm.board.collectAsStateWithLifecycle()
    val stats = remember(data, board.weekStart) { vm.statsForSelectedWeek() }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = "Statistics", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Text(WeekUtils.formatRange(board.weekStart), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(10.dp))
            StatGrid(
                pairs = listOf(
                    "Planned meals" to stats.plannedEntriesThisWeek.toString(),
                    "Filled slots" to stats.filledSlotsThisWeek.toString(),
                    "Empty slots" to stats.emptySlotsThisWeek.toString(),
                    "Unique dishes" to stats.uniqueDishesThisWeek.toString(),
                    "Repeated dishes" to stats.repeatedDishCount.toString(),
                    "Shopping remaining" to stats.shoppingItemsRemaining.toString(),
                    "Meals this month" to stats.mealsPlannedThisMonth.toString(),
                    "Saved templates" to stats.savedTemplateCount.toString(),
                ),
            )

            Spacer(Modifier.height(8.dp))
            Text("Most used slot: ${stats.mostUsedSlot?.displayName ?: "—"}", style = MaterialTheme.typography.bodyMedium)
            Text("Most planned dish: ${stats.mostPlannedDishName ?: "—"}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(14.dp))
            SectionLabel("Filled slots per day")
            FilledSlotStrip(stats.filledSlotStrip)

            Spacer(Modifier.height(14.dp))
            SectionLabel("Meal-slot distribution")
            val maxSlot = (stats.slotDistribution.values.maxOrNull() ?: 0).coerceAtLeast(1)
            MealSlot.ordered.forEach { slot ->
                val count = stats.slotDistribution[slot] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(slot.displayName, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelLarge)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / maxSlot.toFloat())
                                .height(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(slot.accentColor()),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatGrid(pairs: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pairs.chunked(2).forEach { rowPairs ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPairs.forEach { (label, value) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (rowPairs.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FilledSlotStrip(perDay: List<Int>) {
    val labels = listOf("1", "2", "3", "4", "5", "6", "7")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        perDay.forEachIndexed { index, filled ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((70 * (filled.coerceIn(0, 4)) / 4).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(labels.getOrElse(index) { "" }, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
