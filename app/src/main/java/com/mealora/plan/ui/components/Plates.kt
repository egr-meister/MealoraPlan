package com.mealora.plan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mealora.plan.model.MealEntry
import com.mealora.plan.model.MealSlot
import com.mealora.plan.ui.theme.EmptyPlateBorder
import com.mealora.plan.ui.theme.accentColor
import com.mealora.plan.ui.theme.plateTint

/**
 * A ceramic-plate graphic drawn purely with Compose shapes (no images). An outer
 * rim ring plus an inner well. When [filled] is false it renders as an outlined
 * empty plate.
 */
@Composable
fun PlateGraphic(
    slot: MealSlot,
    filled: Boolean,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val accent = slot.accentColor()
    val well = if (filled) slot.plateTint() else Color.Transparent
    Canvas(modifier = modifier.size(diameter)) {
        val d = size.minDimension
        val rimStroke = d * 0.09f
        val radius = d / 2f - rimStroke / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        // Inner well fill
        drawCircle(color = well, radius = radius * 0.72f, center = center)
        // Inner well ring
        drawCircle(
            color = if (filled) accent else EmptyPlateBorder,
            radius = radius * 0.72f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = rimStroke * 0.6f),
        )
        // Outer rim
        drawCircle(
            color = if (filled) accent else EmptyPlateBorder,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = rimStroke),
        )
    }
}

/**
 * A meal plate card for one slot on the Day board. Handles both the empty
 * (outlined plate + "Add Meal") and filled (dish labels + indicators) states.
 */
@Composable
fun MealPlateCard(
    slot: MealSlot,
    entries: List<MealEntry>,
    labelFor: (MealEntry) -> String,
    onAdd: () -> Unit,
    onEntryClick: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filled = entries.isNotEmpty()
    val accent = slot.accentColor()
    val description = if (filled) {
        "${slot.displayName} plate, ${entries.size} planned: " +
            entries.joinToString(", ") { labelFor(it) }
    } else {
        "${slot.displayName} plate, no meal planned. Add meal."
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlateGraphic(slot = slot, filled = filled, diameter = 46.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    slot.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!filled) {
                    Text(
                        "No meal planned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    AddMealPill(onClick = onAdd, accent = accent)
                } else {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        MealEntryRow(entry = entry, label = labelFor(entry), onClick = { onEntryClick(entry) })
                    }
                    Spacer(Modifier.height(8.dp))
                    AddMealPill(onClick = onAdd, accent = accent, compact = true)
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(entry: MealEntry, label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.servingsLabel.isNotBlank()) {
                    Text(
                        entry.servingsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry.note.isNotBlank()) {
                Icon(
                    Icons.Filled.StickyNote2,
                    contentDescription = "Has note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            if (entry.repeatedFromEntryId != null) {
                Icon(
                    Icons.Filled.Loop,
                    contentDescription = "Repeated meal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AddMealPill(onClick: () -> Unit, accent: Color, compact: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (compact) "Add another" else "Add Meal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** A small four-segment plate dot used in the weekly overview to show slot fill. */
@Composable
fun SlotDots(slotFilled: Map<MealSlot, Boolean>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MealSlot.ordered.forEach { slot ->
            val on = slotFilled[slot] == true
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (on) slot.accentColor() else EmptyPlateBorder)
                    .semantics { contentDescription = "${slot.displayName} ${if (on) "planned" else "empty"}" },
            )
        }
    }
}

/** The shopping basket strip shown below the weekly board. */
@Composable
fun ShoppingBasketStrip(itemCount: Int, remaining: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ShoppingBasket, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Shopping list", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (itemCount == 0) "No items yet" else "$remaining of $itemCount to buy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
