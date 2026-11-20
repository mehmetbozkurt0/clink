package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Dark only (spec §9). Surface containers are mapped too, so dialogs and sheets don't fall back to Material grey. */
private val ClinkColorScheme = darkColorScheme(
    primary = ClinkColors.Acid,
    onPrimary = ClinkColors.Ink,
    secondary = ClinkColors.Pink,
    onSecondary = ClinkColors.Ink, // Paper on Pink fails AA
    tertiary = ClinkColors.Acid,
    onTertiary = ClinkColors.Ink,
    background = ClinkColors.Wall,
    onBackground = ClinkColors.Paper,
    surface = ClinkColors.WallRaised,
    onSurface = ClinkColors.Paper,
    surfaceVariant = ClinkColors.WallRaised,
    onSurfaceVariant = ClinkColors.Muted,
    surfaceContainerLowest = ClinkColors.Wall,
    surfaceContainerLow = ClinkColors.WallRaised,
    surfaceContainer = ClinkColors.WallRaised,
    surfaceContainerHigh = ClinkColors.WallRaised,
    surfaceContainerHighest = ClinkColors.WallRaised,
    outline = ClinkColors.Muted,
)

@Composable
fun ClinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClinkColorScheme, typography = clinkTypography(), content = content)
}
