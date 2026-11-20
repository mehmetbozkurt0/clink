package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.game_beerpong
import com.mehmetbozkurt.clink.resources.game_spin
import com.mehmetbozkurt.clink.resources.game_tod
import com.mehmetbozkurt.clink.ui.components.ClinkCard
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Shared layout pieces of the flow screens. */
@Composable
internal fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun ScreenTitle(text: String, overline: String? = null) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
        if (overline != null) {
            Text(text = overline, color = ClinkColors.Acid, style = MaterialTheme.typography.labelLarge)
        }
        Text(text = text, color = ClinkColors.Paper, style = MaterialTheme.typography.headlineSmall)
    }
}

/** A tappable card; disabled cards are dimmed and ignore taps. */
@Composable
internal fun ChoiceCard(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ClinkCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        content = content,
    )
}

@Composable
internal fun GameId.title(): String = stringResource(
    when (this) {
        GameId.TRUTH_OR_DARE -> Res.string.game_tod
        GameId.SPIN_THE_BOTTLE -> Res.string.game_spin
        GameId.BEER_PONG -> Res.string.game_beerpong
    },
)
