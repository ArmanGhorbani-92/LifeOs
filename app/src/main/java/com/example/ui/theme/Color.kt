package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Core LIFEOS Brand Palette (exact specification)
val LifeOsWhite = Color(0xFFFFFFFF)
val LifeOsOlivePrimary = Color(0xFF71805A)
val LifeOsOliveDarkAccent = Color(0xFF8C9E72)
val LifeOsOliveLight = Color(0xFFE5EAD9)
val LifeOsNavyPrimary = Color(0xFF17263F)
val LifeOsLightBackground = Color(0xFFF4F5F0)
val LifeOsDarkBackground = Color(0xFF101B2D)
val LifeOsDarkSurface = Color(0xFF1B2A40)
val LifeOsSecondaryText = Color(0xFF788294)
val LifeOsDarkSecondaryText = Color(0xFFA6B0C2)

// Translucent Glass Borders (exact specification)
val LifeOsLightBorder = Color(0x1A17263F) // rgba(23,38,63,0.10)
val LifeOsDarkBorder = Color(0x1AFFFFFF)  // rgba(255,255,255,0.10)

// Harmonized Semantic Accents (restrained within Olive + Navy visual identity)
val SemanticSuccessLight = Color(0xFF5A784B)
val SemanticSuccessDark = Color(0xFF8FB07C)
val SemanticWarningLight = Color(0xFFB07D3A)
val SemanticWarningDark = Color(0xFFD6A45C)
val SemanticDangerLight = Color(0xFFB24A4A)
val SemanticDangerDark = Color(0xFFE07A7A)
val SemanticInfoLight = Color(0xFF3E5C76)
val SemanticInfoDark = Color(0xFF7FA3C4)

@Immutable
data class LifeOsExtendedColors(
    val isDark: Boolean,
    val backgroundBase: Color,
    val backgroundOrbPrimary: Color,
    val backgroundOrbSecondary: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassSurfaceSubtle: Color,
    val glassBorder: Color,
    val glassHighlightTop: Color,
    val oliveAccent: Color,
    val oliveSoftContainer: Color,
    val navyStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val chartPalette: List<Color>
)

val LightLifeOsColors = LifeOsExtendedColors(
    isDark = false,
    backgroundBase = LifeOsLightBackground,
    backgroundOrbPrimary = Color(0xFFE5EAD9),
    backgroundOrbSecondary = Color(0xFFE3E8F0),
    glassSurface = Color(0xD9FFFFFF),         // 85% frosted white
    glassSurfaceElevated = Color(0xEBFFFFFF), // 92% crisp glass
    glassSurfaceSubtle = Color(0x99E5EAD9),   // 60% soft olive tint
    glassBorder = LifeOsLightBorder,
    glassHighlightTop = Color(0x99FFFFFF),
    oliveAccent = LifeOsOlivePrimary,
    oliveSoftContainer = LifeOsOliveLight,
    navyStrong = LifeOsNavyPrimary,
    textPrimary = LifeOsNavyPrimary,
    textSecondary = LifeOsSecondaryText,
    divider = Color(0x1417263F),
    success = SemanticSuccessLight,
    warning = SemanticWarningLight,
    danger = SemanticDangerLight,
    info = SemanticInfoLight,
    chartPalette = listOf(
        LifeOsOlivePrimary,
        LifeOsNavyPrimary,
        Color(0xFF8C9E72),
        Color(0xFF3E5C76),
        Color(0xFFB07D3A),
        Color(0xFF788294)
    )
)

val DarkLifeOsColors = LifeOsExtendedColors(
    isDark = true,
    backgroundBase = LifeOsDarkBackground,
    backgroundOrbPrimary = Color(0xFF1F314A),
    backgroundOrbSecondary = Color(0xFF213028),
    glassSurface = Color(0xCC1B2A40),         // 80% frosted dark navy
    glassSurfaceElevated = Color(0xE022344E), // 88% elevated dark glass
    glassSurfaceSubtle = Color(0x4071805A),   // 25% olive dark tint
    glassBorder = LifeOsDarkBorder,
    glassHighlightTop = Color(0x14FFFFFF),
    oliveAccent = LifeOsOliveDarkAccent,
    oliveSoftContainer = Color(0xFF27352B),
    navyStrong = LifeOsWhite,
    textPrimary = LifeOsWhite,
    textSecondary = LifeOsDarkSecondaryText,
    divider = Color(0x14FFFFFF),
    success = SemanticSuccessDark,
    warning = SemanticWarningDark,
    danger = SemanticDangerDark,
    info = SemanticInfoDark,
    chartPalette = listOf(
        LifeOsOliveDarkAccent,
        Color(0xFF7FA3C4),
        Color(0xFFAAB994),
        Color(0xFFD6A45C),
        Color(0xFFE07A7A),
        Color(0xFFA6B0C2)
    )
)
