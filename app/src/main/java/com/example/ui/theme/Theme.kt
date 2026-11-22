package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val JarvisDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = VoidBlack,
    primaryContainer = HudSurfaceElevated,
    onPrimaryContainer = ArcCyanSoft,
    secondary = ArcBlueDeep,
    onSecondary = MetallicSilver,
    secondaryContainer = HudSurface,
    onSecondaryContainer = MetallicSilver,
    tertiary = MetallicSilver,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = MetallicSilver,
    surface = DeepSpace,
    onSurface = MetallicSilver,
    surfaceVariant = HudSurface,
    onSurfaceVariant = SilverMuted,
    outline = HudBorder,
    outlineVariant = ColorTokens.SubtleBorder,
    error = StatusDanger,
    onError = VoidBlack,
)

private object ColorTokens {
    val SubtleBorder = HudBorder.copy(alpha = 0.5f)
}

val JarvisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content,
    )
}
