package com.mealora.plan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mealora.plan.model.MealSlot
import com.mealora.plan.ui.Copy
import com.mealora.plan.ui.components.DisclaimerBanner
import com.mealora.plan.ui.components.PlateGraphic

/** First-launch onboarding. No mascot; a simplified plate board is the hero. */
@Composable
fun OnboardingScreen(onPlanWeek: () -> Unit, onExplore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MealSlot.ordered.forEach { slot ->
                PlateGraphic(slot = slot, filled = true, diameter = 54.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Mealora Plan", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Build your week one plate at a time.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        OnboardingPoint("Weekly plate board", "Plan Breakfast, Lunch, Dinner, and Snacks on a seven-day board.")
        OnboardingPoint("Your own dishes", "Create personal dishes and reuse them across the week.")
        OnboardingPoint("Repeat and copy", "Repeat a meal on several days, copy a day, or reuse a whole week.")
        OnboardingPoint("Shopping list", "Build a shopping list from ingredients you enter.")
        OnboardingPoint("Weekly templates", "Save a week as a template and apply it later.")
        OnboardingPoint("Menu history", "Review previous weekly menus any time.")
        OnboardingPoint("Offline storage", "Your meal plans stay on this device.")

        Spacer(Modifier.height(12.dp))
        DisclaimerBanner(
            "Mealora Plan does not calculate calories or provide dietary, medical, weight-loss, or " +
                "nutritional advice.",
        )
        Spacer(Modifier.height(10.dp))
        DisclaimerBanner(Copy.MANUAL_PLANNING_DISCLAIMER)

        Spacer(Modifier.height(24.dp))
        Button(onClick = onPlanWeek, modifier = Modifier.fillMaxWidth()) { Text("Plan This Week") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) { Text("Explore Board") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPoint(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
