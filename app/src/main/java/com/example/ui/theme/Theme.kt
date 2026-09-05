package com.example.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

data class CustomThemeColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val primary: Color,
    val primaryLight: Color,
    val text: Color,
    val textMuted: Color,
    val accentName: String
)

val LocalThemeColors = staticCompositionLocalOf {
    CustomThemeColors(
        background = Color(0xFF09090B),
        surface = Color(0xFF18181B),
        border = Color(0xFF27272A),
        primary = Color(0xFFFF2E56),
        primaryLight = Color(0x2BFF2E56),
        text = Color(0xFFFAFAFA),
        textMuted = Color(0xFFA1A1AA),
        accentName = "red"
    )
}

fun getCustomThemeColors(isDark: Boolean, accentName: String): CustomThemeColors {
    val primaryColor = when (accentName.lowercase()) {
        "blue" -> if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
        "purple" -> if (isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED)
        "green" -> if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
        "orange" -> if (isDark) Color(0xFFF97316) else Color(0xFFEA580C)
        "pink" -> if (isDark) Color(0xFFEC4899) else Color(0xFFDB2777)
        "teal" -> if (isDark) Color(0xFF14B8A6) else Color(0xFF0D9488)
        "gold" -> if (isDark) Color(0xFFF59E0B) else Color(0xFFCA8A04)
        else -> if (isDark) Color(0xFFFF2E56) else Color(0xFFDC2626) // default red "red"
    }
    
    val primaryLight = when (accentName.lowercase()) {
        "blue" -> if (isDark) Color(0x2B3B82F6) else Color(0x2B2563EB)
        "purple" -> if (isDark) Color(0x2B8B5CF6) else Color(0x2B7C3AED)
        "green" -> if (isDark) Color(0x2B22C55E) else Color(0x2B16A34A)
        "orange" -> if (isDark) Color(0x2BF97316) else Color(0x2BEA580C)
        "pink" -> if (isDark) Color(0x2BEC4899) else Color(0x2BDB2777)
        "teal" -> if (isDark) Color(0x2B14B8A6) else Color(0x2B0D9488)
        "gold" -> if (isDark) Color(0x2BF59E0B) else Color(0x2BCA8A04)
        else -> if (isDark) Color(0x2BFF2E56) else Color(0x2BDC2626) // default red
    }

    return if (isDark) {
        CustomThemeColors(
            background = Color(0xFF09090B),
            surface = Color(0xFF18181B),
            border = Color(0xFF27272A),
            primary = primaryColor,
            primaryLight = primaryLight,
            text = Color(0xFFFAFAFA),
            textMuted = Color(0xFFA1A1AA),
            accentName = accentName
        )
    } else {
        CustomThemeColors(
            background = Color(0xFFFAFAFA),
            surface = Color(0xFFF4F4F5),
            border = Color(0xFFE4E4E7),
            primary = primaryColor,
            primaryLight = primaryLight,
            text = Color(0xFF09090B),
            textMuted = Color(0xFF717172),
            accentName = accentName
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark",
    accentName: String = "red",
    densityMode: String = "standard",
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val themeColors = getCustomThemeColors(isDark, accentName)
    val density = LocalDensity.current
    val customDensity = Density(
        density = when (densityMode.lowercase()) {
            "compact" -> density.density * 0.82f
            "large" -> density.density * 1.15f
            else -> density.density * 1.0f
        },
        fontScale = density.fontScale
    )

    CompositionLocalProvider(
        LocalThemeColors provides themeColors,
        LocalDensity provides customDensity
    ) {
        Crossfade(targetState = themeColors, label = "ThemeCrossfade", animationSpec = tween(500)) { colors ->
            val materialScheme = if (isDark) {
                darkColorScheme(
                    primary = colors.primary,
                    secondary = colors.surface,
                    tertiary = Color(0xFFF59E0B), // GoldenHour
                    background = colors.background,
                    surface = colors.surface,
                    onPrimary = Color.White,
                    onSecondary = colors.text,
                    onTertiary = Color(0xFF09090B), // ObsidianBlack
                    onBackground = colors.text,
                    onSurface = colors.text,
                    outline = colors.border
                )
            } else {
                lightColorScheme(
                    primary = colors.primary,
                    secondary = colors.surface,
                    tertiary = Color(0xFFF59E0B),
                    background = colors.background,
                    surface = colors.surface,
                    onPrimary = Color.White,
                    onSecondary = colors.text,
                    onTertiary = Color(0xFFFAFAFA),
                    onBackground = colors.text,
                    onSurface = colors.text,
                    outline = colors.border
                )
            }

            MaterialTheme(
                colorScheme = materialScheme,
                typography = Typography,
                content = content
            )
        }
    }
}
