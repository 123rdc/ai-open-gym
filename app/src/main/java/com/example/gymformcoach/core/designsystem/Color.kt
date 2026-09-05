package com.example.gymformcoach.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** §16: the full set of colors a screen actually reads. */
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color
)

private val DarkBase = ThemeColors(
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    primary = Color(0xFFD0FD3E), // Neon Green - the default identity, unchanged
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    error = Color(0xFFFF453A)
)

private val LightBase = ThemeColors(
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFFD0FD3E),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6E6E73),
    error = Color(0xFFD32F2F)
)

/**
 * §16: accent options. Key is what's persisted in PreferenceManager.accent;
 * "neon" is the default identity and must never be removed or renumbered.
 */
object AccentPalette {
    val options: Map<String, Color> = linkedMapOf(
        "neon" to Color(0xFFD0FD3E),
        "electricBlue" to Color(0xFF4DA6FF),
        "coral" to Color(0xFFFF6B5B),
        "violet" to Color(0xFFB388FF),
        "amber" to Color(0xFFFFC107),
        "mint" to Color(0xFF4DD8B0),
        "magenta" to Color(0xFFFF4DA6)
    )

    fun colorFor(key: String): Color = options[key] ?: options.getValue("neon")
}

fun resolveThemeColors(mode: String, accentKey: String): ThemeColors {
    val base = if (mode == "light") LightBase else DarkBase
    return base.copy(primary = AccentPalette.colorFor(accentKey))
}

val LocalThemeColors = compositionLocalOf { DarkBase }

// Transparent to every existing call site (`color = Primary`, `tint = Background`, ...) -
// each is a @Composable-context property read, not a constant, so it now resolves through
// whatever GymFormCoachTheme provided instead of a hardcoded value (§16: "All new UI must
// read theme colors, never hardcode hex values" - existing call sites get this for free).
val Background: Color @Composable get() = LocalThemeColors.current.background
val Surface: Color @Composable get() = LocalThemeColors.current.surface
val Primary: Color @Composable get() = LocalThemeColors.current.primary
val TextPrimary: Color @Composable get() = LocalThemeColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalThemeColors.current.textSecondary
val Error: Color @Composable get() = LocalThemeColors.current.error
