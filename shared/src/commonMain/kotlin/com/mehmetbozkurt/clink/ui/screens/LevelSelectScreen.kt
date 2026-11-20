package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.level_casual_desc
import com.mehmetbozkurt.clink.resources.level_hard_desc
import com.mehmetbozkurt.clink.resources.level_spicy_desc
import com.mehmetbozkurt.clink.resources.level_title
import com.mehmetbozkurt.clink.ui.components.AdultConfirmDialog
import androidx.compose.ui.text.font.FontWeight
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.needsAdultConfirmation
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Spicy asks for the 18+ confirmation every time it is tapped. */
@Composable
fun LevelSelectScreen(onLevel: (Level) -> Unit, onBack: () -> Unit) {
    var askAdult by remember { mutableStateOf(false) }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.level_title))
        // Scrolls when long descriptions or a large font don't fit.
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Level.entries.forEachIndexed { index, level ->
                ChoiceCard(
                    onClick = { if (level.needsAdultConfirmation()) askAdult = true else onLevel(level) },
                    rotation = PosterTilts[index % PosterTilts.size],
                ) {
                    LevelTag(level)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = level.description(),
                        color = ClinkColors.Ink,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }

    if (askAdult) {
        AdultConfirmDialog(
            onConfirm = {
                askAdult = false
                onLevel(Level.SPICY)
            },
            onDismiss = { askAdult = false },
        )
    }
}

@Composable
private fun Level.description(): String = stringResource(
    when (this) {
        Level.CASUAL -> Res.string.level_casual_desc
        Level.HARD -> Res.string.level_hard_desc
        Level.SPICY -> Res.string.level_spicy_desc
    },
)
