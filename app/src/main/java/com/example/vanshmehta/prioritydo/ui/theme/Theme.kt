package com.example.vanshmehta.prioritydo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Sun.Golden,
    onPrimary = Sun.Navy,
    primaryContainer = Sun.SoftYellow,
    onPrimaryContainer = Sun.Navy,
    secondary = Sun.Amber,
    onSecondary = Sun.Navy,
    secondaryContainer = Sun.SoftYellow,
    onSecondaryContainer = Sun.Navy,
    tertiary = Sun.Coral,
    onTertiary = Sun.Navy,
    background = Sun.Background,
    onBackground = Sun.Navy,
    surface = Sun.Card,
    onSurface = Sun.Navy,
    surfaceVariant = Sun.SoftYellow,
    onSurfaceVariant = Sun.TextSecondary,
    outline = Sun.TextSecondary,
    outlineVariant = Sun.Divider,
    error = Sun.Error,
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = Sun.Golden,
    onPrimary = Sun.Navy,
    primaryContainer = Sun.DarkSurfaceHigh,
    onPrimaryContainer = Sun.Golden,
    secondary = Sun.Amber,
    onSecondary = Sun.Navy,
    secondaryContainer = Sun.DarkSurfaceHigh,
    onSecondaryContainer = Sun.DarkText,
    tertiary = Sun.Coral,
    onTertiary = Sun.Navy,
    background = Sun.DarkBackground,
    onBackground = Sun.DarkText,
    surface = Sun.DarkSurface,
    onSurface = Sun.DarkText,
    surfaceVariant = Sun.DarkSurfaceHigh,
    onSurfaceVariant = Sun.DarkTextSecondary,
    outline = Sun.DarkTextSecondary,
    outlineVariant = Sun.DarkSurfaceHigh,
    error = Sun.DarkError,
    onError = Sun.Navy
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    displayMedium = TextStyle(fontSize = 52.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PriorityDoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
