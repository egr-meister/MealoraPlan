package com.mealora.plan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Warm "Ceramic Weekly Table" light theme. Mealora Plan intentionally uses a
 * single warm light scheme (no dynamic color) so the plate board identity is
 * consistent on every device.
 */
private val MealoraColorScheme = lightColorScheme(
    primary = TableTerracotta,
    onPrimary = PlateWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = DeepTerracotta,
    secondary = DeepTerracotta,
    onSecondary = PlateWhite,
    tertiary = LunchSage,
    onTertiary = PlateWhite,
    background = AppBackground,
    onBackground = DeepText,
    surface = SurfaceWhite,
    onSurface = DeepText,
    surfaceVariant = WarmCream,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor,
    outlineVariant = EmptyPlateBorder,
    error = ErrorRed,
    onError = PlateWhite,
)

@Composable
fun MealoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MealoraColorScheme,
        typography = MealoraTypography,
        content = content,
    )
}
