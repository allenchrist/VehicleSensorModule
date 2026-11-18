package com.example.vehiclesensormodule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The neutral colours, which change between dark and light mode. Dark mode
 * matches the web dashboard's original look, so the phone and car screen
 * look like one product.
 */
data class AppPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceCard: Color,
    val surfaceRaised: Color,
    val outlineSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val DarkPalette = AppPalette(
    isDark = true,
    background = Color(0xFF070B10),
    surfaceCard = Color(0xFF0E141B),
    surfaceRaised = Color(0xFF18212B),
    outlineSubtle = Color(0xFF27323E),
    textPrimary = Color(0xFFF2F5F7),
    textSecondary = Color(0xFFAEB9C3),
    textMuted = Color(0xFF87949F)
)

// Matches the website's light mode
val LightPalette = AppPalette(
    isDark = false,
    background = Color(0xFFF4F6F9),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEEF2F6),
    outlineSubtle = Color(0xFFD6DDE4),
    textPrimary = Color(0xFF101820),
    textSecondary = Color(0xFF48566A),
    textMuted = Color(0xFF6B7886)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Background: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.background

val SurfaceCard: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.surfaceCard

val SurfaceRaised: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.surfaceRaised

val OutlineSubtle: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.outlineSubtle

val TextPrimary: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.textPrimary

val TextSecondary: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.textSecondary

val TextMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current.textMuted

// The same in both modes
val Accent = Color(0xFF56C7FF)
val AccentDeep = Color(0xFF1E88C7)
val OnAccent = Color(0xFF04202E)

val Success = Color(0xFF43D17A)
val Warning = Color(0xFFF2B84B)
val Danger = Color(0xFFFF5C63)
