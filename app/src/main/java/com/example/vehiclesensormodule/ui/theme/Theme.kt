package com.example.vehiclesensormodule.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Dark or light mode, chosen from the account dialog and remembered on this
 * phone. Dark is the original look.
 */
object AppTheme {
    private const val PREFERENCES = "settings"
    private const val LIGHT_KEY = "light_mode"

    var isLight by mutableStateOf(false)
        private set

    fun load(context: Context) {
        isLight = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(LIGHT_KEY, false)
    }

    fun setLight(context: Context, light: Boolean) {
        isLight = light

        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putBoolean(LIGHT_KEY, light)
        }
    }
}

private fun colorSchemeFor(palette: AppPalette): ColorScheme = if (palette.isDark) {
    darkColorScheme(
        primary = Accent,
        onPrimary = OnAccent,
        secondary = Accent,
        onSecondary = OnAccent,
        tertiary = Success,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surfaceCard,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceRaised,
        onSurfaceVariant = palette.textSecondary,
        surfaceContainerHigh = palette.surfaceRaised,
        outline = palette.outlineSubtle,
        outlineVariant = palette.outlineSubtle,
        error = Danger,
        onError = OnAccent
    )
} else {
    // A deeper blue for text buttons, so they read on white
    lightColorScheme(
        primary = AccentDeep,
        onPrimary = OnAccent,
        secondary = AccentDeep,
        onSecondary = OnAccent,
        tertiary = Success,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surfaceCard,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceRaised,
        onSurfaceVariant = palette.textSecondary,
        surfaceContainerHigh = palette.surfaceRaised,
        outline = palette.outlineSubtle,
        outlineVariant = palette.outlineSubtle,
        error = Danger,
        onError = OnAccent
    )
}

@Composable
fun VehicleSensorModuleTheme(light: Boolean = AppTheme.isLight, content: @Composable () -> Unit) {
    val palette = if (light) LightPalette else DarkPalette

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette),
            typography = Typography,
            content = content
        )
    }
}
