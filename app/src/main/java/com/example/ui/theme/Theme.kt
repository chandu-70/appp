package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoftBg,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = SlateInk,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = SlateInk,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    tertiaryContainer = IndigoSoftBg,
    onTertiaryContainer = Color(0xFF312E81),
    error = CoralDebt,
    onError = Color.White,
    errorContainer = CoralSoftBg,
    onErrorContainer = Color(0xFF7F1D1D),
    background = BgOffWhite,
    onBackground = SlateInk,
    surface = SurfaceWhite,
    onSurface = SlateInk,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SlateMuted,
    outline = BorderSlate,
    outlineVariant = Color(0xFFCBD5E1),
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFCBD5E1),
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFE0E7FF),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF475569),
)

val SmartSplitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmartSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SmartSplitShapes,
        content = content,
    )
}
