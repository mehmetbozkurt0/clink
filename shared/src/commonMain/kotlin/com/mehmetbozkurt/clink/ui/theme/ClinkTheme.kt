package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Dark only (spec §9). Surface containers are mapped too, so dialogs and sheets don't fall back to Material grey. */
private val ClinkColorScheme = darkColorScheme(
    primary = ClinkColors.Primary,
    onPrimary = ClinkColors.OnPrimary,
    secondary = ClinkColors.Secondary,
    onSecondary = ClinkColors.TextPrimary,
    tertiary = ClinkColors.Mint,
    onTertiary = ClinkColors.OnPrimary,
    background = ClinkColors.Background,
    onBackground = ClinkColors.TextPrimary,
    surface = ClinkColors.Surface,
    onSurface = ClinkColors.TextPrimary,
    surfaceVariant = ClinkColors.SurfaceRaised,
    onSurfaceVariant = ClinkColors.TextMuted,
    surfaceContainerLowest = ClinkColors.Background,
    surfaceContainerLow = ClinkColors.Surface,
    surfaceContainer = ClinkColors.Surface,
    surfaceContainerHigh = ClinkColors.Surface,
    surfaceContainerHighest = ClinkColors.SurfaceRaised,
    outline = ClinkColors.TextMuted,
)

@Composable
fun ClinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClinkColorScheme, typography = clinkTypography(), content = content)
}
