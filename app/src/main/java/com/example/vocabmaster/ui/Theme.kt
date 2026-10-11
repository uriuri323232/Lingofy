package com.example.vocabmaster.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4F46E5)
val Violet = Color(0xFF7C3AED)
val Amber = Color(0xFFF59E0B)
val Success = Color(0xFF16A34A)
val Danger = Color(0xFFDC2626)

private val Light = lightColorScheme(
    primary = Indigo, onPrimary = Color.White, primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Amber, onSecondary = Color(0xFF1F2937), secondaryContainer = Color(0xFFFEF3C7), onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF14B8A6), background = Color(0xFFF6F6FB), onBackground = Color(0xFF111827),
    surface = Color(0xFFF6F6FB), onSurface = Color(0xFF111827), surfaceVariant = Color(0xFFEAEAF4), onSurfaceVariant = Color(0xFF52525B),
    surfaceContainerLow = Color.White, surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFEFEFF8),
    outlineVariant = Color(0xFFD9D9E8)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA5B4FC), onPrimary = Color(0xFF1E1B4B), primaryContainer = Color(0xFF3730A3), onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFFBBF24), onSecondary = Color(0xFF1F2937), secondaryContainer = Color(0xFF78350F), onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF5EEAD4), background = Color(0xFF0E0F1C), onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF0E0F1C), onSurface = Color(0xFFF3F4F6), surfaceVariant = Color(0xFF26283D), onSurfaceVariant = Color(0xFFB4B7CC),
    surfaceContainerLow = Color(0xFF181A2E), surfaceContainer = Color(0xFF181A2E), surfaceContainerHigh = Color(0xFF22243A),
    outlineVariant = Color(0xFF34364F)
)

@Composable
fun LingofyTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
