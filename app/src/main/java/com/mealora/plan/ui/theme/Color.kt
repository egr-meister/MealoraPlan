package com.mealora.plan.ui.theme

import androidx.compose.ui.graphics.Color
import com.mealora.plan.model.MealSlot
import com.mealora.plan.model.ShoppingCategory

// Primary identity
val TableTerracotta = Color(0xFFC87355)
val DeepTerracotta = Color(0xFF9A513B)
val WarmCream = Color(0xFFFFF6E8)
val PlateWhite = Color(0xFFFFFDFC)
val PrimaryContainer = Color(0xFFF3D9CC)

// Meal slot colors
val BreakfastHoney = Color(0xFFE4A83B)
val LunchSage = Color(0xFF7FA27A)
val DinnerIndigo = Color(0xFF6575A6)
val SnackRose = Color(0xFFC77B8D)

// Neutrals
val AppBackground = Color(0xFFF3EEE6)
val SurfaceWhite = Color(0xFFFFFFFF)
val DeepText = Color(0xFF282522)
val SecondaryText = Color(0xFF6D6862)
val DividerColor = Color(0xFFD9D1C7)
val EmptyPlateBorder = Color(0xFFC9C1B7)

// State
val FilledSlot = Color(0xFF4F8A68)
val EmptySlot = Color(0xFF8E918F)
val ShoppingPending = Color(0xFFC1842E)
val ShoppingChecked = Color(0xFF718077)
val ErrorRed = Color(0xFFB64A47)

/** Accent color for each meal slot. Always paired with text labels, never color alone. */
fun MealSlot.accentColor(): Color = when (this) {
    MealSlot.Breakfast -> BreakfastHoney
    MealSlot.Lunch -> LunchSage
    MealSlot.Dinner -> DinnerIndigo
    MealSlot.Snack -> SnackRose
}

/** A soft tinted plate surface for each meal slot. */
fun MealSlot.plateTint(): Color = when (this) {
    MealSlot.Breakfast -> Color(0xFFFBEFD6)
    MealSlot.Lunch -> Color(0xFFE8F0E6)
    MealSlot.Dinner -> Color(0xFFE4E8F2)
    MealSlot.Snack -> Color(0xFFF6E4E9)
}

/** A neutral accent per shopping category (used sparingly as a small dot). */
fun ShoppingCategory.accentColor(): Color = when (this) {
    ShoppingCategory.Produce -> LunchSage
    ShoppingCategory.Dairy -> Color(0xFF6E86B0)
    ShoppingCategory.MeatAndFish -> Color(0xFFB0655A)
    ShoppingCategory.Bakery -> BreakfastHoney
    ShoppingCategory.Pantry -> DeepTerracotta
    ShoppingCategory.Frozen -> Color(0xFF5E9AA8)
    ShoppingCategory.Drinks -> DinnerIndigo
    ShoppingCategory.Snacks -> SnackRose
    ShoppingCategory.Household -> SecondaryText
    ShoppingCategory.Other -> SecondaryText
}
