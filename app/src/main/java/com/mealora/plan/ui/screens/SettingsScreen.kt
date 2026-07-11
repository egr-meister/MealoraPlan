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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.mealora.plan.model.FirstDayOfWeek
import com.mealora.plan.model.MealSlot
import com.mealora.plan.ui.Copy
import com.mealora.plan.ui.PlannerViewModel
import com.mealora.plan.ui.components.ConfirmDialog
import com.mealora.plan.ui.components.DisclaimerBanner
import com.mealora.plan.ui.components.MealoraTopBar
import com.mealora.plan.ui.components.SectionLabel

@Composable
fun SettingsScreen(vm: PlannerViewModel, navController: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val settings = data.settings
    val prompts = settings.promptSettings

    var confirm by remember { mutableStateOf<ConfirmKind?>(null) }

    Column(Modifier.fillMaxSize()) {
        MealoraTopBar(title = "Settings", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {

            SectionLabel("First day of week")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FirstDayOfWeek.entries.forEach { d ->
                    FilterChip(selected = settings.firstDayOfWeek == d, onClick = { vm.updateSettings { it.copy(firstDayOfWeek = d) } }, label = { Text(d.name) })
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionLabel("Default meal slot")
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealSlot.ordered.forEach { s ->
                    FilterChip(selected = settings.defaultMealSlot == s, onClick = { vm.updateSettings { it.copy(defaultMealSlot = s) } }, label = { Text(s.displayName) })
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingSwitch("Open current week by default", settings.openCurrentWeekByDefault) { on -> vm.updateSettings { it.copy(openCurrentWeekByDefault = on) } }
            SettingSwitch("Show empty meal slots", settings.showEmptyMealSlots) { on -> vm.updateSettings { it.copy(showEmptyMealSlots = on) } }

            Spacer(Modifier.height(10.dp))
            SectionLabel("In-app prompts")
            SettingSwitch("Enable in-app prompts", prompts.enabled) { on -> vm.updateSettings { it.copy(promptSettings = it.promptSettings.copy(enabled = on)) } }
            SettingSwitch("Today has empty slots", prompts.showEmptyToday, enabled = prompts.enabled) { on -> vm.updateSettings { it.copy(promptSettings = it.promptSettings.copy(showEmptyToday = on)) } }
            SettingSwitch("Tomorrow has no meals", prompts.showEmptyTomorrow, enabled = prompts.enabled) { on -> vm.updateSettings { it.copy(promptSettings = it.promptSettings.copy(showEmptyTomorrow = on)) } }
            SettingSwitch("Next week is empty", prompts.showEmptyNextWeek, enabled = prompts.enabled) { on -> vm.updateSettings { it.copy(promptSettings = it.promptSettings.copy(showEmptyNextWeek = on)) } }
            SettingSwitch("No shopping list yet", prompts.showShoppingPrompt, enabled = prompts.enabled) { on -> vm.updateSettings { it.copy(promptSettings = it.promptSettings.copy(showShoppingPrompt = on)) } }
            Spacer(Modifier.height(6.dp))
            DisclaimerBanner(Copy.PROMPTS_EXPLANATION)

            Spacer(Modifier.height(12.dp))
            SectionLabel("Manage data")
            OutlinedButton(onClick = { vm.showOnboardingAgain() }, modifier = Modifier.fillMaxWidth()) { Text("Show onboarding again") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { vm.clearCheckedShopping() }, modifier = Modifier.fillMaxWidth()) { Text("Clear checked shopping items") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { confirm = ConfirmKind.DeleteWeek }, modifier = Modifier.fillMaxWidth()) { Text("Delete selected weekly plan") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { confirm = ConfirmKind.DeleteHistory }, modifier = Modifier.fillMaxWidth()) { Text("Delete all menu history") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { confirm = ConfirmKind.DeleteArchived }, modifier = Modifier.fillMaxWidth()) { Text("Delete archived dishes") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { confirm = ConfirmKind.ResetAll },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Reset all local data") }

            Spacer(Modifier.height(16.dp))
            SectionLabel("About")
            Text("Mealora Plan", style = MaterialTheme.typography.titleMedium)
            Text("Version 1.0.0 · Offline weekly meal planner", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))
            SectionLabel("Manual planning")
            DisclaimerBanner(Copy.MANUAL_PLANNING_DISCLAIMER)
            Spacer(Modifier.height(8.dp))
            SectionLabel("Ingredients")
            DisclaimerBanner(Copy.INGREDIENT_DISCLAIMER)
            Spacer(Modifier.height(8.dp))
            SectionLabel("Privacy")
            DisclaimerBanner(Copy.PRIVACY_NOTE)
            Spacer(Modifier.height(24.dp))
        }
    }

    when (confirm) {
        ConfirmKind.DeleteWeek -> ConfirmDialog(
            title = "Delete selected weekly plan?",
            message = "The currently selected week and its meals will be removed. Other weeks are not affected.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteWeek(vm.selectedWeekStart()); confirm = null },
            onDismiss = { confirm = null },
        )
        ConfirmKind.DeleteHistory -> ConfirmDialog(
            title = "Delete all menu history?",
            message = "All weekly plans and meals except the currently selected week will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteAllHistory(); confirm = null },
            onDismiss = { confirm = null },
        )
        ConfirmKind.DeleteArchived -> ConfirmDialog(
            title = "Delete archived dishes?",
            message = "Archived dishes that are not used by any plan or template will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteArchivedDishes(); confirm = null },
            onDismiss = { confirm = null },
        )
        ConfirmKind.ResetAll -> ConfirmDialog(
            title = Copy.RESET_TITLE,
            message = Copy.RESET_MESSAGE,
            confirmLabel = "Reset everything",
            onConfirm = { vm.resetAll(); confirm = null },
            onDismiss = { confirm = null },
        )
        null -> {}
    }
}

private enum class ConfirmKind { DeleteWeek, DeleteHistory, DeleteArchived, ResetAll }

@Composable
private fun SettingSwitch(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
