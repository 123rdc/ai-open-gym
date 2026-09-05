package com.example.gymformcoach.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.gymformcoach.core.utils.PreferenceManager

/**
 * §16: dark (the default identity) or light, with a selectable accent. Reads
 * PreferenceManager directly rather than taking mode/accent as parameters, since
 * every existing call site just does `GymFormCoachTheme { ... }` and settings
 * changes should apply without touching every one of them.
 */
@Composable
fun GymFormCoachTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val themeColors = remember(prefs.themeMode, prefs.accent) {
        resolveThemeColors(prefs.themeMode, prefs.accent)
    }
    val isLight = prefs.themeMode == "light"

    val colorScheme = remember(themeColors, isLight) {
        if (isLight) {
            lightColorScheme(
                primary = themeColors.primary,
                onPrimary = themeColors.background,
                background = themeColors.background,
                onBackground = themeColors.textPrimary,
                surface = themeColors.surface,
                onSurface = themeColors.textPrimary,
                secondary = themeColors.textSecondary,
                error = themeColors.error
            )
        } else {
            darkColorScheme(
                primary = themeColors.primary,
                onPrimary = themeColors.background,
                background = themeColors.background,
                onBackground = themeColors.textPrimary,
                surface = themeColors.surface,
                onSurface = themeColors.textPrimary,
                secondary = themeColors.textSecondary,
                error = themeColors.error
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
