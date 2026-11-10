package com.example.vehiclesensormodule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    tertiary = Success,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = SurfaceRaised,
    outline = OutlineSubtle,
    outlineVariant = OutlineSubtle,
    error = Danger,
    onError = OnAccent
)

// Always dark, to match the car screen and stay easy to read in a vehicle
@Composable
fun VehicleSensorModuleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
