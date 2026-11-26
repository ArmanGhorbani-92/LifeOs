package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.preferences.ThemeMode

val LocalLifeOsColors = staticCompositionLocalOf { LightLifeOsColors }

private val LifeOsLightColorScheme = lightColorScheme(
    primary = LifeOsOlivePrimary,
    onPrimary = LifeOsWhite,
    primaryContainer = LifeOsOliveLight,
    onPrimaryContainer = LifeOsNavyPrimary,
    secondary = LifeOsNavyPrimary,
    onSecondary = LifeOsWhite,
    secondaryContainer = ColorTokens.LightSecondaryContainer,
    onSecondaryContainer = LifeOsNavyPrimary,
    tertiary = SemanticInfoLight,
    onTertiary = LifeOsWhite,
    background = LifeOsLightBackground,
    onBackground = LifeOsNavyPrimary,
    surface = LifeOsWhite,
    onSurface = LifeOsNavyPrimary,
    surfaceVariant = LifeOsOliveLight,
    onSurfaceVariant = LifeOsSecondaryText,
    outline = LifeOsLightBorder,
    outlineVariant = LifeOsLightBorder,
    error = SemanticDangerLight,
    onError = LifeOsWhite
)

private val LifeOsDarkColorScheme = darkColorScheme(
    primary = LifeOsOliveDarkAccent,
    onPrimary = LifeOsDarkBackground,
    primaryContainer = ColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = LifeOsOliveLight,
    secondary = LifeOsWhite,
    onSecondary = LifeOsDarkBackground,
    secondaryContainer = LifeOsDarkSurface,
    onSecondaryContainer = LifeOsWhite,
    tertiary = SemanticInfoDark,
    onTertiary = LifeOsDarkBackground,
    background = LifeOsDarkBackground,
    onBackground = LifeOsWhite,
    surface = LifeOsDarkSurface,
    onSurface = LifeOsWhite,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = LifeOsDarkSecondaryText,
    outline = LifeOsDarkBorder,
    outlineVariant = LifeOsDarkBorder,
    error = SemanticDangerDark,
    onError = LifeOsDarkBackground
)

private object ColorTokens {
    val LightSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE6EAF0)
    val DarkPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2B3A2C)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF22344D)
}

val LifeOsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

object LifeOsTheme {
    val colors: LifeOsExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLifeOsColors.current
}

@Composable
fun LifeOsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) LifeOsDarkColorScheme else LifeOsLightColorScheme
    val extendedColors = if (darkTheme) DarkLifeOsColors else LightLifeOsColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalLifeOsColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeOsTypography,
            shapes = LifeOsShapes,
            content = content
        )
    }
}
